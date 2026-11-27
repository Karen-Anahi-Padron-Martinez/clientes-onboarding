package com.kapm.onboarding_clientes.exception;

import lombok.Getter;

/**
 * Excepción lanzada cuando PuntoRed responde y rechaza una operación (autenticación o transacción).
 * El resultado de la operación es CONOCIDO (rechazada por el proveedor).
 */
@Getter
public class PuntoRedRechazoException extends PuntoRedIntegracionException {

    /** NUM_AUTORIZACION que PuntoRed devuelve aun en un rechazo (p. ej. "-1"); puede ser null. */
    private String numAutorizacion;

    public PuntoRedRechazoException(String codigoPuntoRed, String textoPuntoRed) {
        super(
                ErrorMessage.PUNTORED_TRANSACCION_RECHAZADA.getCodigo(),
                "PuntoRed rechazó la operación [Código: " + codigoPuntoRed + "]: " + textoPuntoRed,
                codigoPuntoRed,
                textoPuntoRed
        );
    }

    public PuntoRedRechazoException(String mensaje, String codigoPuntoRed, String textoPuntoRed) {
        super(
                ErrorMessage.PUNTORED_TRANSACCION_RECHAZADA.getCodigo(),
                mensaje,
                codigoPuntoRed,
                textoPuntoRed
        );
    }

    /** Rechazo de sendTx que conserva el NUM_AUTORIZACION de la respuesta. */
    public static PuntoRedRechazoException conAutorizacion(String codigoPuntoRed, String textoPuntoRed, String numAutorizacion) {
        PuntoRedRechazoException e = new PuntoRedRechazoException(codigoPuntoRed, textoPuntoRed);
        e.numAutorizacion = numAutorizacion;
        return e;
    }
}
