package com.kapm.onboarding_clientes.service;

import com.kapm.onboarding_clientes.client.puntored.PuntoRedClient;
import com.kapm.onboarding_clientes.client.puntored.model.PuntoRedTxResponse;
import com.kapm.onboarding_clientes.client.puntored.model.SendTxPayload;
import com.kapm.onboarding_clientes.config.PuntoRedProperties;
import com.kapm.onboarding_clientes.dto.request.AplicarPagoRequest;
import com.kapm.onboarding_clientes.dto.request.EmitirCodigo2FARequest;
import com.kapm.onboarding_clientes.dto.request.SolicitarCodigoPagoRequest;
import com.kapm.onboarding_clientes.dto.request.ValidarCodigo2FARequest;
import com.kapm.onboarding_clientes.dto.response.ClienteResponse;
import com.kapm.onboarding_clientes.dto.response.CodigoVerificacionResponse;
import com.kapm.onboarding_clientes.dto.response.PagoResponse;
import com.kapm.onboarding_clientes.dto.response.SolicitarCodigoPagoResponse;
import com.kapm.onboarding_clientes.entity.EstatusCuenta;
import com.kapm.onboarding_clientes.entity.EstatusPago;
import com.kapm.onboarding_clientes.entity.PagoServicio;
import com.kapm.onboarding_clientes.entity.Saldo;
import com.kapm.onboarding_clientes.entity.catalogo.CatalogoProductoPuntoRed;
import com.kapm.onboarding_clientes.exception.*;
import com.kapm.onboarding_clientes.repository.CatalogoProductoPuntoRedRepository;
import com.kapm.onboarding_clientes.repository.PagoServicioRepository;
import com.kapm.onboarding_clientes.repository.SaldoRepository;
import com.kapm.onboarding_clientes.validation.CurpRfcUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PagoServicioServiceImpl implements PagoServicioService {

    private static final DateTimeFormatter FORMATTER_HORA_LOCAL = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final PagoServicioRepository pagoServicioRepository;
    private final CatalogoProductoPuntoRedRepository catalogoProductoRepository;
    private final PuntoRedProperties puntoRedProperties;
    private final PuntoRedClient puntoRedClient;
    private final ClienteService clienteService;
    private final SaldoRepository saldoRepository;
    private final CodigoVerificacionService codigoVerificacionService;
    private final PagoTransaccionalService pagoTransaccionalService;

    @Override
    @Transactional(readOnly = true)
    public SendTxPayload armarPayloadDesdePago(PagoServicio pago) {
        if (pago == null) {
            throw new ReglaNegocioException("La entidad PagoServicio no puede ser nula");
        }

        String upc = pago.getUpc();

        // Sin producto en catálogo se deja nulo: SendTxPayload.validar() infiere el tipo por la presencia de referencia
        Integer tipoFront = catalogoProductoRepository.findByIdProducto(pago.getIdProducto())
                .map(CatalogoProductoPuntoRed::getTipoFront)
                .orElse(null);

        String horaLocal = obtenerHoraLocalConfigurada();
        String unidad = pago.getUnidad() != null ? pago.getUnidad() : obtenerUnidadConfigurada();

        SendTxPayload payload = SendTxPayload.builder()
                .idProducto(pago.getIdProducto())
                .idServicio(pago.getIdServicio())
                .telefono(pago.getTelefono())
                .horaLocal(horaLocal)
                .referencia(pago.getReferencia())
                .montoPago(pago.getMonto())
                .upc(upc)
                .unidad(unidad)
                .tipoFront(tipoFront)
                .build();

        payload.validar();
        log.info("SendTxPayload armado para verificación reutilizando UPC: {}", upc);
        return payload;
    }

    private String obtenerHoraLocalConfigurada() {
        String zonaStr = (puntoRedProperties.getZonaHoraria() != null && !puntoRedProperties.getZonaHoraria().isBlank())
                ? puntoRedProperties.getZonaHoraria().trim()
                : "America/Mexico_City";

        ZoneId zoneId;
        try {
            zoneId = ZoneId.of(zonaStr);
        } catch (Exception e) {
            log.warn("Zona horaria inválida '{}', utilizando 'America/Mexico_City' por defecto", zonaStr);
            zoneId = ZoneId.of("America/Mexico_City");
        }

        return ZonedDateTime.now(zoneId).format(FORMATTER_HORA_LOCAL);
    }

    private String obtenerUnidadConfigurada() {
        String unidad = puntoRedProperties.getUnidad();
        if (unidad == null || unidad.trim().length() < 3 || unidad.trim().length() > 25) {
            throw new PuntoRedIntegracionException("La propiedad 'puntored.unidad' es requerida y debe tener entre 3 y 25 caracteres");
        }
        return unidad.trim();
    }

    // =========================================================================
    // FLUJO DE PAGO / RECARGA EN DOS PASOS (2FA + BLOQUEO PESIMISTA)
    // =========================================================================

    @Override
    public SolicitarCodigoPagoResponse solicitarCodigo(SolicitarCodigoPagoRequest request, String username) {
        if (request == null) {
            throw new ReglaNegocioException("La solicitud de emisión de código no puede ser nula");
        }

        DatosOperacionValidada op = validarOperacion(
                clienteService.obtenerCurpPorUsername(username),
                request.getCurp(),
                request.getRfc(),
                request.getNumeroCuenta(),
                request.getIdProducto(),
                request.getIdServicio(),
                request.getTelefono(),
                request.getReferencia(),
                request.getMonto()
        );

        EmitirCodigo2FARequest emitirReq = EmitirCodigo2FARequest.builder()
                .numeroCuenta(request.getNumeroCuenta().trim())
                .curp(op.cliente().getCurp())
                .idProducto(request.getIdProducto())
                .idServicio(request.getIdServicio())
                .telefono(op.telefono())
                .referencia(op.referencia())
                .monto(op.monto())
                .build();

        CodigoVerificacionResponse codigoResp = codigoVerificacionService.emitirCodigo(emitirReq);

        return SolicitarCodigoPagoResponse.builder()
                .mensaje("Código de verificación enviado al correo registrado")
                .correoEnmascarado(codigoResp.getCorreoEnmascarado())
                .numeroCuenta(request.getNumeroCuenta().trim())
                .idProducto(request.getIdProducto())
                .idServicio(request.getIdServicio())
                .monto(op.monto())
                .expiraEn(codigoResp.getExpiraEn())
                .build();
    }

    @Override
    public PagoResponse aplicarPago(AplicarPagoRequest request, String username) {
        if (request == null) {
            throw new ReglaNegocioException("La solicitud de aplicación de pago no puede ser nula");
        }

        if (request.getIdempotencyKey() == null || request.getIdempotencyKey().isBlank()) {
            throw new ReglaNegocioException("La llave de idempotencia (idempotencyKey) es obligatoria");
        }
        String idempotencyKey = request.getIdempotencyKey().trim();

        // 0. Titular autenticado: todo lo que sigue debe ser sobre SU cliente y SU cuenta
        String curpTitular = clienteService.obtenerCurpPorUsername(username);

        // 6. Si llega el mismo idempotencyKey otra vez, devolver el resultado del primero sin cobrar de nuevo
        Optional<PagoServicio> pagoExistente = pagoServicioRepository.findByIdempotencyKey(idempotencyKey);
        if (pagoExistente.isPresent()) {
            return responderIdempotente(pagoExistente.get(), request, curpTitular);
        }

        // 1. Repetir las validaciones del paso 1 (no confiar en que ya se hicieron)
        DatosOperacionValidada op = validarOperacion(
                curpTitular,
                request.getCurp(),
                request.getRfc(),
                request.getNumeroCuenta(),
                request.getIdProducto(),
                request.getIdServicio(),
                request.getTelefono(),
                request.getReferencia(),
                request.getMonto()
        );

        // 2. Validar el código 2FA
        if (request.getCodigo() == null || !request.getCodigo().trim().matches("^\\d{6}$")) {
            throw new ReglaNegocioException(ErrorMessage.CODIGO_2FA_INVALIDO);
        }

        ValidarCodigo2FARequest validarReq = ValidarCodigo2FARequest.builder()
                .numeroCuenta(request.getNumeroCuenta().trim())
                .curp(op.cliente().getCurp())
                .codigo(request.getCodigo().trim())
                .idProducto(request.getIdProducto())
                .idServicio(request.getIdServicio())
                .telefono(op.telefono())
                .referencia(op.referencia())
                .monto(op.monto())
                .build();

        codigoVerificacionService.validarCodigo(validarReq);

        // 3. Pre-verificar que el proveedor está disponible (token en caché o renovado) ANTES de reservar saldo.
        // Si falla aquí, la transacción nunca se envió: se responde 503 sin tocar el saldo del cliente.
        try {
            puntoRedClient.obtenerToken();
        } catch (PuntoRedIntegracionException e) {
            throw new ProveedorNoDisponibleException(e);
        }

        // 4. En UNA transacción con @Lock(PESSIMISTIC_WRITE) sobre Saldo:
        // re-verificar saldo, bajar saldo_disponible (reserva), crear PagoServicio en PENDIENTE
        String horaLocal = obtenerHoraLocalConfigurada();
        String unidad = obtenerUnidadConfigurada();

        PagoServicio pagoPendiente;
        try {
            pagoPendiente = pagoTransaccionalService.reservarSaldoYCrearPago(
                    request.getNumeroCuenta().trim(),
                    op.cliente().getCurp(),
                    op.cliente().getRfc(),
                    request.getIdProducto(),
                    request.getIdServicio(),
                    op.telefono(),
                    op.referencia(),
                    op.monto(),
                    idempotencyKey,
                    horaLocal,
                    unidad
            );
        } catch (DataIntegrityViolationException e) {
            // Dos peticiones simultáneas con la misma llave: el índice único de idempotency_key deja pasar solo una.
            // La transacción perdedora se revierte completa (incluida la reserva de saldo).
            PagoServicio ganador = pagoServicioRepository.findByIdempotencyKey(idempotencyKey)
                    .orElseThrow(() -> e);
            return responderIdempotente(ganador, request, curpTitular);
        }

        // 5. FUERA de esa transacción: armar SendTxPayload, cifrar, obtener token y llamar sendTx
        SendTxPayload payload = SendTxPayload.builder()
                .idProducto(pagoPendiente.getIdProducto())
                .idServicio(pagoPendiente.getIdServicio())
                .telefono(pagoPendiente.getTelefono())
                .horaLocal(pagoPendiente.getHoraLocal())
                .referencia(pagoPendiente.getReferencia())
                .montoPago(pagoPendiente.getMonto())
                .upc(pagoPendiente.getUpc())
                .unidad(pagoPendiente.getUnidad())
                .tipoFront(op.producto().getTipoFront())
                .build();

        // 6. Resultado, cada uno en su propia transacción corta con bloqueo:
        try {
            PuntoRedTxResponse txResponse = puntoRedClient.sendTx(payload);

            // Éxito: PagoServicio EXITOSO y bajar saldo_contable
            PagoServicio pagoExitoso = pagoTransaccionalService.aplicarExito(
                    pagoPendiente.getId(),
                    request.getNumeroCuenta().trim(),
                    pagoPendiente.getMonto(),
                    txResponse
            );
            return mapearAPagoResponse(pagoExitoso, "Pago procesado exitosamente por PuntoRed");

        } catch (PuntoRedRechazoException e) {
            // Incluye el CODIGO 06 en este PRIMER envío: el UPC es nuevo, así que PuntoRed detectó un duplicado de OTRA
            // operación (mismo servicio y monto en 24 h) y esta no se cobró. Solo en un reenvío del mismo UPC el 06
            // significa "ya aplicada" (ver VerificacionPagosServiceImpl).
            String codRechazo = e.getCodigoPuntoRed() != null ? e.getCodigoPuntoRed() : String.valueOf(e.getCodigo());
            String txtRechazo = e.getTextoPuntoRed() != null ? e.getTextoPuntoRed() : e.getMessage();
            log.warn("Transacción rechazada por PuntoRed [upc: {}, codigo: {}, texto: {}]",
                    pagoPendiente.getUpc(), codRechazo, txtRechazo);

            // Rechazo confirmado: RECHAZADO y devolver saldo_disponible
            PagoServicio pagoRechazado = pagoTransaccionalService.aplicarRechazo(
                    pagoPendiente.getId(),
                    request.getNumeroCuenta().trim(),
                    pagoPendiente.getMonto(),
                    codRechazo,
                    txtRechazo
            );
            return mapearAPagoResponse(pagoRechazado, ErrorMessage.PUNTORED_TRANSACCION_RECHAZADA.getMensaje());

        } catch (Exception e) {
            // Cualquier otra falla (timeout, 5xx, XML ilegible, error inesperado) puede ocurrir DESPUÉS de que PuntoRed
            // procesó el cargo: el resultado es DESCONOCIDO. Nunca se devuelve el saldo ni se reintenta automáticamente;
            // el pago queda PENDIENTE y el scheduler lo verifica con confirmTx o reenviando el mismo UPC.
            log.error("Resultado DESCONOCIDO de sendTx [upc: {}, causa: {}]. Pago PENDIENTE con saldo reservado.",
                    pagoPendiente.getUpc(), e.getClass().getSimpleName());

            String motivo = e instanceof PuntoRedTimeoutException ? "TIMEOUT_PROVEEDOR"
                    : e instanceof PuntoRedRespuestaInvalidaException ? "RESPUESTA_INVALIDA_PROVEEDOR"
                    : "ERROR_COMUNICACION_PROVEEDOR";
            String idTxProveedor = e instanceof PuntoRedTimeoutException timeout ? timeout.getIdTx() : null;
            PagoServicio pagoPendientePersistido = pagoTransaccionalService.registrarTimeout(pagoPendiente.getId(), motivo, idTxProveedor);
            return mapearAPagoResponse(pagoPendientePersistido, "Pago en proceso. Pendiente de confirmación por el proveedor");
        }
    }

    private DatosOperacionValidada validarOperacion(
            String curpTitular,
            String curp,
            String rfc,
            String numeroCuenta,
            Integer idProducto,
            Integer idServicio,
            String telefono,
            String referencia,
            BigDecimal monto) {

        if (numeroCuenta == null || numeroCuenta.isBlank()) {
            throw new ReglaNegocioException(ErrorMessage.CUENTA_NO_ENCONTRADA);
        }

        // 1. Identificación por curp o rfc
        ClienteResponse cliente;
        if (curp != null && !curp.isBlank()) {
            if (!CurpRfcUtil.esCurpValida(curp)) {
                throw new ReglaNegocioException(ErrorMessage.CURP_FORMATO_INVALIDO);
            }
            cliente = clienteService.obtenerClientePorCurp(CurpRfcUtil.sanitizarCurp(curp));
        } else if (rfc != null && !rfc.isBlank()) {
            if (!CurpRfcUtil.esRfcValido(rfc)) {
                throw new ReglaNegocioException(ErrorMessage.RFC_FORMATO_INVALIDO);
            }
            cliente = clienteService.obtenerClientePorRfc(CurpRfcUtil.sanitizarRfc(rfc));
        } else {
            throw new ReglaNegocioException(ErrorMessage.IDENTIFICADOR_REQUERIDO);
        }

        // 1.1 El cliente indicado en el body debe ser el titular del token (403 sin revelar si existe)
        if (!cliente.getCurp().equals(curpTitular)) {
            throw new AccesoDenegadoException();
        }

        // 2. Cliente activo
        if (!Boolean.TRUE.equals(cliente.getActivo())) {
            throw new ReglaNegocioException(ErrorMessage.CLIENTE_INACTIVO);
        }

        // 3. Cuenta que pertenece a ese cliente y está ACTIVA
        if (cliente.getCuenta() == null || !numeroCuenta.trim().equals(cliente.getCuenta().getNumeroCuenta())) {
            throw new ReglaNegocioException(ErrorMessage.CUENTA_NO_PERTENECE_CLIENTE);
        }
        if (cliente.getCuenta().getEstatus() == null || !EstatusCuenta.ACTIVA.equals(cliente.getCuenta().getEstatus())) {
            throw new ReglaNegocioException(ErrorMessage.CUENTA_INACTIVA);
        }

        // 4. Catálogo: producto y servicio existen y están activos en CatalogoProductoPuntoRed
        if (idProducto == null || idProducto <= 0 || idServicio == null || idServicio <= 0) {
            throw new ReglaNegocioException(ErrorMessage.PRODUCTO_NO_PERMITIDO);
        }
        CatalogoProductoPuntoRed producto = catalogoProductoRepository.findByIdProducto(idProducto)
                .orElseThrow(() -> new ReglaNegocioException(ErrorMessage.PRODUCTO_NO_PERMITIDO));

        if (!Boolean.TRUE.equals(producto.getActivo()) || !producto.getIdServicio().equals(idServicio)) {
            throw new ReglaNegocioException(ErrorMessage.PRODUCTO_NO_PERMITIDO);
        }

        int tipoFront = producto.getTipoFront() != null ? producto.getTipoFront() : -1;
        // SOLO se permiten recargas (0) y pagos de servicios (1 ó 2); cualquier otro producto se rechaza
        if (tipoFront != 0 && tipoFront != 1 && tipoFront != 2) {
            throw new ReglaNegocioException(ErrorMessage.PRODUCTO_NO_PERMITIDO);
        }

        String telFinal;
        String refFinal;

        if (tipoFront == 0) {
            // Recarga TAE: requiere teléfono celular válido (10 dígitos)
            if (telefono == null || !telefono.trim().matches("^\\d{10}$")) {
                throw new ReglaNegocioException(ErrorMessage.TELEFONO_REQUERIDO_RECARGA);
            }
            telFinal = telefono.trim();
            refFinal = null;
        } else {
            // Pago de servicios: requiere referencia válida de 1 a 100 caracteres
            if (referencia == null || referencia.trim().isEmpty() || referencia.trim().length() > 100) {
                throw new ReglaNegocioException(ErrorMessage.REFERENCIA_REQUERIDA_SERVICIO);
            }
            refFinal = referencia.trim();
            // Teléfono para sendTx: si se provee se valida 10-20 dígitos/chars; si no, se usa el móvil del cliente
            if (telefono != null && !telefono.isBlank()) {
                if (telefono.trim().length() < 10 || telefono.trim().length() > 20) {
                    throw new ReglaNegocioException("El teléfono debe tener entre 10 y 20 caracteres");
                }
                telFinal = telefono.trim();
            } else {
                telFinal = cliente.getTelefonoMovil() != null ? cliente.getTelefonoMovil().trim() : "5500000000";
            }
        }

        // 5. Monto > 0 y máx 2 decimales
        if (monto == null || monto.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ReglaNegocioException("El monto debe ser mayor a cero");
        }
        if (monto.scale() > 2) {
            throw new ReglaNegocioException("El monto no puede tener más de 2 decimales");
        }
        BigDecimal montoFinal = monto.setScale(2, RoundingMode.HALF_UP);
        validarRangoMonto(producto, montoFinal);

        // 6. Saldo disponible suficiente (sin descontar todavía)
        Saldo saldo = saldoRepository.findByCuentaNumeroCuenta(numeroCuenta.trim())
                .orElseThrow(() -> new RecursoNoEncontradoException(ErrorMessage.CUENTA_NO_ENCONTRADA, numeroCuenta));

        if (saldo.getSaldoDisponible() == null || saldo.getSaldoDisponible().compareTo(montoFinal) < 0) {
            throw new ReglaNegocioException(ErrorMessage.SALDO_INSUFICIENTE);
        }

        return new DatosOperacionValidada(cliente, producto, montoFinal, telFinal, refFinal);
    }

    /**
     * Valida el monto contra las restricciones del producto (o los límites globales si el producto no las define).
     * Evita montos manipulados: el cliente no puede enviar un monto distinto al permitido para un paquete de monto fijo.
     */
    private void validarRangoMonto(CatalogoProductoPuntoRed producto, BigDecimal monto) {
        BigDecimal minimo = producto.getMontoMinimo() != null ? producto.getMontoMinimo() : puntoRedProperties.getMontoMinimoOperacion();
        BigDecimal maximo = producto.getMontoMaximo() != null ? producto.getMontoMaximo() : puntoRedProperties.getMontoMaximoOperacion();

        if ((minimo != null && monto.compareTo(minimo) < 0) || (maximo != null && monto.compareTo(maximo) > 0)) {
            throw new ReglaNegocioException(ErrorMessage.MONTO_FUERA_DE_RANGO);
        }
    }

    /**
     * Responde una llave de idempotencia ya registrada SOLO si pertenece a la misma operación (misma cuenta,
     * producto y monto). Si la llave se reutiliza para otra operación se responde 409, lo que además evita
     * exponer pagos de otra cuenta a quien adivine una llave.
     */
    private PagoResponse responderIdempotente(PagoServicio existente, AplicarPagoRequest request, String curpTitular) {
        if (!curpTitular.equals(existente.getCurp())) {
            throw new AccesoDenegadoException();
        }
        boolean mismaOperacion = request.getNumeroCuenta() != null
                && request.getNumeroCuenta().trim().equals(existente.getNumeroCuenta())
                && existente.getIdProducto().equals(request.getIdProducto())
                && request.getMonto() != null
                && existente.getMonto().compareTo(request.getMonto()) == 0;

        if (!mismaOperacion) {
            log.warn("Llave de idempotencia reutilizada con datos distintos [upc original: {}]", existente.getUpc());
            throw new RecursoDuplicadoException(ErrorMessage.IDEMPOTENCY_KEY_REUTILIZADA);
        }

        log.info("Petición idempotente detectada [upc: {}]. Retornando transacción previa sin cobrar nuevamente.", existente.getUpc());
        return mapearAPagoResponse(existente, "Resultado de la transacción previa (Idempotente)");
    }

    private PagoResponse mapearAPagoResponse(PagoServicio pago, String mensaje) {
        return PagoResponse.desde(pago, mensaje);
    }

    private record DatosOperacionValidada(
            ClienteResponse cliente,
            CatalogoProductoPuntoRed producto,
            BigDecimal monto,
            String telefono,
            String referencia
    ) {}
}
