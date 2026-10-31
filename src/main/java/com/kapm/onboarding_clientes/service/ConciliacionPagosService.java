package com.kapm.onboarding_clientes.service;

import com.kapm.onboarding_clientes.dto.request.ResolverConciliacionRequest;
import com.kapm.onboarding_clientes.dto.response.PagoResponse;

import java.util.List;

/**
 * Conciliación MANUAL de pagos que la verificación automática ({@link VerificacionPagosService}) no pudo resolver:
 * fuera de la ventana segura de reenvío, confirmTx vencido o intentos agotados.
 * El saldo permanece reservado hasta que un operador confirma el resultado con PuntoRed.
 */
public interface ConciliacionPagosService {

    List<PagoResponse> listarEnConciliacion();

    /**
     * Cierra un pago abierto: si el proveedor lo procesó se liquida el saldo contable;
     * si no, se libera la reserva del saldo disponible.
     */
    PagoResponse resolver(ResolverConciliacionRequest request);
}
