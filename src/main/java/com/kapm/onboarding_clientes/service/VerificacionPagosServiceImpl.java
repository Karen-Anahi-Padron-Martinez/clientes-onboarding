package com.kapm.onboarding_clientes.service;

import com.kapm.onboarding_clientes.client.puntored.PuntoRedClient;
import com.kapm.onboarding_clientes.client.puntored.model.ConfirmacionTx;
import com.kapm.onboarding_clientes.client.puntored.model.PuntoRedMensaje;
import com.kapm.onboarding_clientes.client.puntored.model.PuntoRedTxResponse;
import com.kapm.onboarding_clientes.client.puntored.model.SendTxPayload;
import com.kapm.onboarding_clientes.config.PuntoRedProperties;
import com.kapm.onboarding_clientes.dto.response.PagoResponse;
import com.kapm.onboarding_clientes.entity.EstatusPago;
import com.kapm.onboarding_clientes.entity.PagoServicio;
import com.kapm.onboarding_clientes.entity.catalogo.CatalogoProductoPuntoRed;
import com.kapm.onboarding_clientes.exception.*;
import com.kapm.onboarding_clientes.repository.CatalogoProductoPuntoRedRepository;
import com.kapm.onboarding_clientes.repository.PagoServicioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class VerificacionPagosServiceImpl implements VerificacionPagosService {

    enum Resultado { LIQUIDADO, LIBERADO, ESCALADO, SIN_CAMBIO }

    private final PagoServicioRepository pagoServicioRepository;
    private final CatalogoProductoPuntoRedRepository catalogoProductoRepository;
    private final PagoTransaccionalService pagoTransaccionalService;
    private final PagoServicioService pagoServicioService;
    private final PuntoRedClient puntoRedClient;
    private final PuntoRedProperties properties;

    @Override
    public int verificarPendientes() {
        LocalDateTime limite = LocalDateTime.now().minusMinutes(properties.getMinutosAntesDeVerificar());
        List<PagoServicio> candidatos = pagoServicioRepository.findByEstatusAndFechaCreacionBefore(EstatusPago.PENDIENTE, limite);

        // Cada pago se resuelve en sus propias transacciones cortas: una falla puntual no bloquea al resto
        int cambios = 0;
        for (PagoServicio pago : candidatos) {
            try {
                if (verificarPago(pago) != Resultado.SIN_CAMBIO) {
                    cambios++;
                }
            } catch (RuntimeException e) {
                log.error("No fue posible verificar el pago [upc: {}, causa: {}]", pago.getUpc(), e.getClass().getSimpleName());
            }
        }
        return cambios;
    }

    @Override
    public PagoResponse verificar(String upc) {
        PagoServicio pago = pagoServicioRepository.findByUpc(upc.trim())
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el pago a verificar"));
        if (pago.getEstatus() != EstatusPago.PENDIENTE && pago.getEstatus() != EstatusPago.EN_CONCILIACION) {
            throw new ReglaNegocioException(ErrorMessage.PAGO_NO_CONCILIABLE);
        }

        Resultado resultado = verificarPago(pago);
        PagoServicio actualizado = pagoServicioRepository.findByUpc(pago.getUpc()).orElse(pago);
        return PagoResponse.desde(actualizado, "Verificación con el proveedor: " + resultado);
    }

    Resultado verificarPago(PagoServicio pago) {
        long minutos = Duration.between(pago.getFechaCreacion(), LocalDateTime.now()).toMinutes();
        if (pago.getIdTxProveedor() != null && !pago.getIdTxProveedor().isBlank()) {
            return verificarConConfirmTx(pago, minutos);
        }
        return verificarReenviandoMismoUpc(pago, minutos);
    }

    private Resultado verificarConConfirmTx(PagoServicio pago, long minutos) {
        if (minutos >= properties.getMinutosVigenciaConfirmTx()) {
            return escalar(pago, "confirmTx ya no es válido (más de 24 h)");
        }

        ConfirmacionTx confirmacion;
        try {
            confirmacion = puntoRedClient.confirmTx(pago.getIdTxProveedor());
        } catch (PuntoRedIntegracionException e) {
            return registrarIntentoSinCerteza(pago, e.getClass().getSimpleName());
        }

        return switch (confirmacion.estado()) {
            case APLICADA -> liquidar(pago, confirmacion.respuesta());
            case NO_ENCONTRADA -> liberar(pago, "03", "TRANSACCION_NO_REGISTRADA_CONFIRMTX");
            case PENDIENTE -> registrarIntentoSinCerteza(pago, "CONFIRMTX_PENDIENTE");
        };
    }

    private Resultado verificarReenviandoMismoUpc(PagoServicio pago, long minutos) {
        // Fuera de la ventana de duplicados PuntoRed procesaría el reenvío como una venta NUEVA: doble cargo
        if (minutos >= ventanaReenvioMinutos(pago.getIdProducto())) {
            return escalar(pago, "fuera de la ventana segura de reenvío");
        }

        SendTxPayload payload = pagoServicioService.armarPayloadDesdePago(pago);
        try {
            return liquidar(pago, puntoRedClient.sendTx(payload));

        } catch (PuntoRedTransaccionDuplicadaException e) {
            // Mismo UPC => la transacción original SÍ se aplicó
            PuntoRedTxResponse aplicada = PuntoRedTxResponse.builder()
                    .mensaje(new PuntoRedMensaje(PuntoRedTransaccionDuplicadaException.CODIGO, e.getTextoPuntoRed()))
                    .build();
            return liquidar(pago, aplicada);

        } catch (PuntoRedAutenticacionException e) {
            // No es un veredicto sobre la transacción: no se puede liberar saldo
            return escalar(pago, "rechazo de autenticación con el proveedor");

        } catch (PuntoRedTimeoutException e) {
            if (e.getIdTx() != null) {
                pagoTransaccionalService.registrarTimeout(pago.getId(), "TIMEOUT_REENVIO", e.getIdTx());
            }
            return registrarIntentoSinCerteza(pago, "TIMEOUT_REENVIO");

        } catch (PuntoRedRechazoException e) {
            // Si la original se hubiera aplicado, PuntoRed respondería 06; cualquier otro rechazo confirma que no se cobró
            return liberar(pago, e.getCodigoPuntoRed(), e.getTextoPuntoRed());

        } catch (PuntoRedIntegracionException e) {
            return registrarIntentoSinCerteza(pago, e.getClass().getSimpleName());
        }
    }

    /**
     * Solo los pagos de servicios (tipoFront 1 ó 2) tienen la ventana de 24 h; ante la duda (producto retirado del
     * catálogo o tipo desconocido) se aplica la ventana corta de recargas.
     */
    private long ventanaReenvioMinutos(Integer idProducto) {
        Integer tipoFront = catalogoProductoRepository.findByIdProducto(idProducto)
                .map(CatalogoProductoPuntoRed::getTipoFront)
                .orElse(null);
        boolean esServicio = tipoFront != null && (tipoFront == 1 || tipoFront == 2);
        return esServicio ? properties.getMinutosVentanaReenvioServicio() : properties.getMinutosVentanaReenvioRecarga();
    }

    private Resultado liquidar(PagoServicio pago, PuntoRedTxResponse respuesta) {
        pagoTransaccionalService.aplicarExito(pago.getId(), pago.getNumeroCuenta(), pago.getMonto(), respuesta);
        log.info("Verificación: pago aplicado por el proveedor, saldo liquidado [upc: {}]", pago.getUpc());
        return Resultado.LIQUIDADO;
    }

    private Resultado liberar(PagoServicio pago, String codigo, String texto) {
        pagoTransaccionalService.aplicarRechazo(pago.getId(), pago.getNumeroCuenta(), pago.getMonto(), codigo, texto);
        log.info("Verificación: el proveedor no aplicó el pago, reserva liberada [upc: {}, codigo: {}]", pago.getUpc(), codigo);
        return Resultado.LIBERADO;
    }

    private Resultado registrarIntentoSinCerteza(PagoServicio pago, String motivo) {
        int intentos = pagoTransaccionalService.registrarIntentoVerificacion(pago.getId());
        log.warn("Verificación sin certeza [upc: {}, motivo: {}, intento: {}/{}]",
                pago.getUpc(), motivo, intentos, properties.getMaxIntentosVerificacion());
        if (intentos >= properties.getMaxIntentosVerificacion()) {
            return escalar(pago, "intentos de verificación agotados");
        }
        return Resultado.SIN_CAMBIO;
    }

    private Resultado escalar(PagoServicio pago, String motivo) {
        if (pago.getEstatus() == EstatusPago.EN_CONCILIACION) {
            return Resultado.SIN_CAMBIO;
        }
        log.error("ALERTA CONCILIACIÓN: pago escalado a revisión manual [upc: {}, motivo: {}]", pago.getUpc(), motivo);
        pagoTransaccionalService.marcarEnConciliacion(pago.getId());
        return Resultado.ESCALADO;
    }
}
