package com.kapm.onboarding_clientes.service;

import com.kapm.onboarding_clientes.client.puntored.model.SendTxPayload;
import com.kapm.onboarding_clientes.entity.PagoServicio;


public interface PagoServicioService {

    /**
     * Reconstruye la trama sendTx de un pago existente reutilizando EXACTAMENTE su UPC, para que PuntoRed
     * detecte el reenvío como duplicado (verificación de resultados desconocidos). Nunca genera un UPC nuevo.
     */
    SendTxPayload armarPayloadDesdePago(PagoServicio pago);

    /**
     * PASO 1: Valida la solicitud, cliente activo, cuenta bancaria asociada y saldo disponible.
     * Emite el código 2FA numérico al correo registrado en BD mediante Brevo.
     *
     * @param request  Datos de la operación y titular
     * @param username Usuario autenticado (subject del JWT); la operación debe ser sobre su propio cliente
     * @return SolicitarCodigoPagoResponse con correo enmascarado y expiración
     */
    com.kapm.onboarding_clientes.dto.response.SolicitarCodigoPagoResponse solicitarCodigo(com.kapm.onboarding_clientes.dto.request.SolicitarCodigoPagoRequest request, String username);

    /**
     * PASO 2: Repite validaciones, valida código 2FA, reserva saldo con @Lock pesimista,
     * realiza llamada HTTP a PuntoRed fuera de transacción, y procesa resultado en transacciones cortas.
     * Maneja idempotencia mediante idempotencyKey.
     *
     * @param request  Datos completos de la operación, código 2FA e idempotencyKey
     * @param username Usuario autenticado (subject del JWT); la operación debe ser sobre su propio cliente
     * @return PagoResponse con el estatus y datos de autorización
     */
    com.kapm.onboarding_clientes.dto.response.PagoResponse aplicarPago(com.kapm.onboarding_clientes.dto.request.AplicarPagoRequest request, String username);
}
