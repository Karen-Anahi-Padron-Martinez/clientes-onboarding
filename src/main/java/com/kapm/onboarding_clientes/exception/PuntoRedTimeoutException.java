package com.kapm.onboarding_clientes.exception;

import lombok.Getter;

/**
 * Excepción lanzada cuando ocurre un timeout de conexión/lectura o falla de red
 * durante o después de enviar una transacción (sendTx), o cuando PuntoRed responde CODIGO 82
 * (su propio tiempo de espera de 62 s se agotó).
 * El resultado de la operación en el proveedor es DESCONOCIDO y requiere consulta (confirmTx) o conciliación.
 */
@Getter
public class PuntoRedTimeoutException extends PuntoRedIntegracionException {

    /** ID_TX devuelto por PuntoRed junto con el CODIGO 82; insumo de confirmTx. Nulo en timeouts de red. */
    private final String idTx;

    public PuntoRedTimeoutException(String mensaje) {
        this(mensaje, (Throwable) null);
    }

    public PuntoRedTimeoutException(String mensaje, Throwable cause) {
        super(ErrorMessage.PUNTORED_TIMEOUT.getCodigo(), mensaje, cause);
        this.idTx = null;
    }

    public PuntoRedTimeoutException(String mensaje, String codigoPuntoRed, String textoPuntoRed, String idTx) {
        super(ErrorMessage.PUNTORED_TIMEOUT.getCodigo(), mensaje, codigoPuntoRed, textoPuntoRed);
        this.idTx = idTx;
    }
}
