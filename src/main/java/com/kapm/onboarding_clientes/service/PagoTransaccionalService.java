package com.kapm.onboarding_clientes.service;

import com.kapm.onboarding_clientes.client.puntored.model.PuntoRedTxResponse;
import com.kapm.onboarding_clientes.entity.PagoServicio;

import java.math.BigDecimal;

/**
 * Servicio transaccional auxiliar con propagación REQUIRES_NEW.
 * Permite aislar y limitar el tiempo de retención de bloqueos pesimistas (@Lock PESSIMISTIC_WRITE)
 * sobre la entidad Saldo, garantizando que NUNCA se mantenga un bloqueo en BD durante la llamada HTTP externa a PuntoRed.
 */
public interface PagoTransaccionalService {

    /**
     * En una transacción corta e independiente: adquiere bloqueo pesimista sobre Saldo,
     * re-verifica saldo disponible suficiente, debita saldo_disponible (reserva),
     * genera un código UPC único (máx 15 caracteres) y registra PagoServicio en estatus PENDIENTE.
     */
    PagoServicio reservarSaldoYCrearPago(
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
            String unidad
    );

    /**
     * En una transacción corta e independiente: adquiere bloqueo pesimista sobre Saldo,
     * reduce saldo_contable y actualiza PagoServicio a EXITOSO con los datos de autorización devueltos por PuntoRed.
     */
    PagoServicio aplicarExito(Long pagoId, String numeroCuenta, BigDecimal monto, PuntoRedTxResponse txResponse);

    /**
     * En una transacción corta e independiente: adquiere bloqueo pesimista sobre Saldo,
     * restituye el saldo_disponible reservado y actualiza PagoServicio a RECHAZADO con el código y texto de rechazo.
     */
    PagoServicio aplicarRechazo(Long pagoId, String numeroCuenta, BigDecimal monto, String codigoRechazo, String textoRechazo);

    /**
     * En una transacción corta e independiente: registra el motivo del timeout / respuesta inválida
     * en el registro PagoServicio manteniéndolo en PENDIENTE.
     * IMPORTANTE: El saldo_disponible PERMANECE RESERVADO (NO se devuelve) para que el Scheduler de conciliación
     * lo resuelva sin riesgo de doble gasto.
     *
     * @param idTxProveedor ID_TX devuelto por PuntoRed con el CODIGO 82 (habilita confirmTx); puede ser nulo
     */
    PagoServicio registrarTimeout(Long pagoId, String motivo, String idTxProveedor);

    /**
     * Incrementa el contador de verificaciones automáticas de un pago aún abierto.
     *
     * @return número de intentos acumulados
     */
    int registrarIntentoVerificacion(Long pagoId);

    /**
     * Escala un pago que sigue PENDIENTE a EN_CONCILIACION (sin mover saldo).
     * Es no-op si el pago ya fue cerrado por una respuesta tardía del proveedor.
     */
    void marcarEnConciliacion(Long pagoId);
}
