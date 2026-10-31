package com.kapm.onboarding_clientes.service;

import com.kapm.onboarding_clientes.client.puntored.model.PuntoRedTxResponse;
import com.kapm.onboarding_clientes.entity.EstatusPago;
import com.kapm.onboarding_clientes.entity.PagoServicio;
import com.kapm.onboarding_clientes.entity.Saldo;
import com.kapm.onboarding_clientes.exception.ErrorMessage;
import com.kapm.onboarding_clientes.exception.PuntoRedIntegracionException;
import com.kapm.onboarding_clientes.exception.RecursoNoEncontradoException;
import com.kapm.onboarding_clientes.exception.ReglaNegocioException;
import com.kapm.onboarding_clientes.repository.PagoServicioRepository;
import com.kapm.onboarding_clientes.repository.SaldoRepository;
import com.kapm.onboarding_clientes.util.DatosSensibles;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.Set;

/**
 * Implementación de transacciones cortas con @Lock(PESSIMISTIC_WRITE) sobre Saldo
 * usando propagación Propagation.REQUIRES_NEW.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PagoTransaccionalServiceImpl implements PagoTransaccionalService {

    private final SaldoRepository saldoRepository;
    private final PagoServicioRepository pagoServicioRepository;
    private final UpcGenerator upcGenerator;

    /** Únicos estados desde los que se puede liquidar o revertir: impide doble débito o doble devolución. */
    private static final Set<EstatusPago> ESTADOS_ABIERTOS = EnumSet.of(EstatusPago.PENDIENTE, EstatusPago.EN_CONCILIACION);

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public PagoServicio reservarSaldoYCrearPago(
            String numeroCuenta,
            String curp,
            String rfc,
            Integer idProducto,
            Integer idServicio,
            String telefono,
            String referencia,
            BigDecimal monto,
            String idempotencyKey,
            String horaLocal,
            String unidad) {

        log.info("Adquiriendo bloqueo pesimista en Saldo para reserva [cuenta: {}]", DatosSensibles.enmascarar(numeroCuenta));

        Saldo saldo = saldoRepository.findByCuentaNumeroCuentaWithLock(numeroCuenta)
                .orElseThrow(() -> new RecursoNoEncontradoException(ErrorMessage.CUENTA_NO_ENCONTRADA, numeroCuenta));

        if (saldo.getSaldoDisponible() == null || saldo.getSaldoDisponible().compareTo(monto) < 0) {
            log.warn("Saldo disponible insuficiente para reserva [cuenta: {}]", DatosSensibles.enmascarar(numeroCuenta));
            throw new ReglaNegocioException(ErrorMessage.SALDO_INSUFICIENTE);
        }

        // Bajar saldo disponible (reserva)
        BigDecimal nuevoDisponible = saldo.getSaldoDisponible().subtract(monto);
        saldo.setSaldoDisponible(nuevoDisponible);
        saldoRepository.save(saldo);
        log.info("Saldo disponible reservado exitosamente [cuenta: {}]", DatosSensibles.enmascarar(numeroCuenta));

        // Generar código UPC único (máx. 15 caracteres)
        String upc = generarUpcUnico();

        PagoServicio pago = PagoServicio.builder()
                .upc(upc)
                .idempotencyKey(idempotencyKey)
                .curp(curp)
                .rfc(rfc)
                .numeroCuenta(numeroCuenta)
                .idProducto(idProducto)
                .idServicio(idServicio)
                .telefono(telefono)
                .referencia(referencia)
                .monto(monto)
                .horaLocal(horaLocal)
                .unidad(unidad)
                .estatus(EstatusPago.PENDIENTE)
                .build();

        PagoServicio guardado = pagoServicioRepository.save(pago);
        log.info("Pago registrado en PENDIENTE con bloqueo liberado [id: {}, upc: {}]", guardado.getId(), upc);

        return guardado;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public PagoServicio aplicarExito(Long pagoId, String numeroCuenta, BigDecimal monto, PuntoRedTxResponse txResponse) {
        log.info("Adquiriendo bloqueo pesimista en Saldo para liquidación exitosa [pagoId: {}, cuenta: {}]",
                pagoId, DatosSensibles.enmascarar(numeroCuenta));

        Saldo saldo = saldoRepository.findByCuentaNumeroCuentaWithLock(numeroCuenta)
                .orElseThrow(() -> new RecursoNoEncontradoException(ErrorMessage.CUENTA_NO_ENCONTRADA, numeroCuenta));

        PagoServicio pago = obtenerPagoAbierto(pagoId);

        // Bajar saldo contable
        BigDecimal nuevoContable = saldo.getSaldoContable().subtract(monto);
        saldo.setSaldoContable(nuevoContable);
        if (saldo.getCuenta() != null) {
            saldo.getCuenta().setSaldo(nuevoContable);
        }
        saldoRepository.save(saldo);

        pago.setEstatus(EstatusPago.EXITOSO);
        if (txResponse.getIdTx() != null) {
            pago.setIdTxProveedor(txResponse.getIdTx());
        }
        pago.setNumAutorizacion(txResponse.getNumAutorizacion());
        pago.setCodigoRespuesta(txResponse.getMensaje() != null ? txResponse.getMensaje().getCodigo() : "01");
        pago.setTextoRespuesta(txResponse.getMensaje() != null ? txResponse.getMensaje().getTexto() : "APROBADA");
        pago.setComision(txResponse.getComision());

        PagoServicio actualizado = pagoServicioRepository.save(pago);
        log.info("Pago actualizado a EXITOSO y saldo contable debitado [upc: {}, codigo: {}]",
                actualizado.getUpc(), actualizado.getCodigoRespuesta());

        return actualizado;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public PagoServicio aplicarRechazo(Long pagoId, String numeroCuenta, BigDecimal monto, String codigoRechazo, String textoRechazo) {
        log.warn("Adquiriendo bloqueo pesimista en Saldo para reversión por rechazo confirmado [pagoId: {}, cuenta: {}, codigo: {}]",
                pagoId, DatosSensibles.enmascarar(numeroCuenta), codigoRechazo);

        Saldo saldo = saldoRepository.findByCuentaNumeroCuentaWithLock(numeroCuenta)
                .orElseThrow(() -> new RecursoNoEncontradoException(ErrorMessage.CUENTA_NO_ENCONTRADA, numeroCuenta));

        PagoServicio pago = obtenerPagoAbierto(pagoId);

        // Devolver saldo disponible reservado
        BigDecimal nuevoDisponible = saldo.getSaldoDisponible().add(monto);
        saldo.setSaldoDisponible(nuevoDisponible);
        saldoRepository.save(saldo);

        pago.setEstatus(EstatusPago.RECHAZADO);
        pago.setCodigoRespuesta(codigoRechazo);
        pago.setTextoRespuesta(textoRechazo);

        PagoServicio actualizado = pagoServicioRepository.save(pago);
        log.info("Pago actualizado a RECHAZADO y saldo disponible restituido [upc: {}]", actualizado.getUpc());

        return actualizado;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public PagoServicio registrarTimeout(Long pagoId, String motivo, String idTxProveedor) {
        log.warn("Registrando timeout/falla de respuesta en pago [pagoId: {}, motivo: {}]. Saldo disponible NO es devuelto.",
                pagoId, motivo);

        PagoServicio pago = pagoServicioRepository.findById(pagoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el registro de pago con ID: " + pagoId));

        pago.setEstatus(EstatusPago.PENDIENTE);
        pago.setCodigoRespuesta("PENDIENTE");
        pago.setTextoRespuesta(motivo != null && motivo.length() > 250 ? motivo.substring(0, 250) : motivo);
        if (idTxProveedor != null && !idTxProveedor.isBlank()) {
            pago.setIdTxProveedor(idTxProveedor.trim());
        }

        return pagoServicioRepository.save(pago);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int registrarIntentoVerificacion(Long pagoId) {
        PagoServicio pago = pagoServicioRepository.findById(pagoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el registro de pago con ID: " + pagoId));
        int intentos = (pago.getIntentosVerificacion() != null ? pago.getIntentosVerificacion() : 0) + 1;
        pago.setIntentosVerificacion(intentos);
        pagoServicioRepository.save(pago);
        return intentos;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void marcarEnConciliacion(Long pagoId) {
        PagoServicio pago = pagoServicioRepository.findById(pagoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el registro de pago con ID: " + pagoId));

        // Solo escala lo que sigue PENDIENTE: si la respuesta tardía de PuntoRed ya lo cerró, no se toca
        if (pago.getEstatus() != EstatusPago.PENDIENTE) {
            return;
        }
        pago.setEstatus(EstatusPago.EN_CONCILIACION);
        pagoServicioRepository.save(pago);
        log.error("ALERTA CONCILIACIÓN: pago sin confirmación del proveedor [upc: {}]. Saldo permanece reservado hasta resolución.",
                pago.getUpc());
    }

    private PagoServicio obtenerPagoAbierto(Long pagoId) {
        PagoServicio pago = pagoServicioRepository.findById(pagoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el registro de pago con ID: " + pagoId));
        if (!ESTADOS_ABIERTOS.contains(pago.getEstatus())) {
            log.warn("Intento de liquidar/revertir un pago ya cerrado [upc: {}, estatus: {}]", pago.getUpc(), pago.getEstatus());
            throw new ReglaNegocioException(ErrorMessage.PAGO_NO_CONCILIABLE);
        }
        return pago;
    }

    private String generarUpcUnico() {
        String upc;
        int intentos = 0;
        do {
            upc = upcGenerator.generar();
            intentos++;
            if (intentos > 10) {
                throw new PuntoRedIntegracionException("No fue posible generar un código UPC único tras 10 intentos consecutivos");
            }
        } while (pagoServicioRepository.existsByUpc(upc));

        if (upc.length() > 15) {
            throw new PuntoRedIntegracionException("El código UPC generado excede el límite máximo de 15 caracteres: " + upc);
        }
        return upc;
    }
}
