package com.kapm.onboarding_clientes.service;

import com.kapm.onboarding_clientes.dto.response.PagoResponse;

/**
 * Resolución automática de pagos cuyo resultado en PuntoRed es desconocido (timeout, CODIGO 82, respuesta ilegible).
 * <p>
 * Estrategia, en orden de preferencia:
 * <ol>
 *   <li>Si PuntoRed devolvió ID_TX: consultar confirmTx (válido 24 h). 01 liquida, 03 libera, 04 espera.</li>
 *   <li>Si no hay ID_TX: reenviar sendTx con el MISMO UPC, solo dentro de la ventana en que PuntoRed detecta duplicados
 *   (15 min recargas / 24 h servicios). 01 = se aplicó ahora; 06 = ya se había aplicado; ambos liquidan.
 *   Un rechazo transaccional libera la reserva.</li>
 *   <li>Fuera de ventana, sin certeza tras N intentos, o ante fallas de autenticación: EN_CONCILIACION (manual).</li>
 * </ol>
 * Nunca se libera saldo sin una respuesta del proveedor que lo justifique.
 */
public interface VerificacionPagosService {

    /**
     * Verifica todos los pagos PENDIENTE con antigüedad suficiente.
     *
     * @return número de pagos que cambiaron de estado (liquidados, liberados o escalados)
     */
    int verificarPendientes();

    /** Verificación manual (ADMIN) de un pago abierto identificado por su UPC. */
    PagoResponse verificar(String upc);
}
