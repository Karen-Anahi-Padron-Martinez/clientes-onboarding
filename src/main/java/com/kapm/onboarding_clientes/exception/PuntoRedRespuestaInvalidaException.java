package com.kapm.onboarding_clientes.exception;

import lombok.Getter;

/**
 * Excepción lanzada cuando la respuesta devuelta por PuntoRed es ilegible,
 * está vacía o no cumple con el formato XML esperado.
 * El resultado de la operación en el proveedor es DESCONOCIDO.
 */
@Getter
public class PuntoRedRespuestaInvalidaException extends PuntoRedIntegracionException {

    public PuntoRedRespuestaInvalidaException(String mensaje) {
        super(ErrorMessage.PUNTORED_RESPUESTA_INVALIDA.getCodigo(), mensaje);
    }

    public PuntoRedRespuestaInvalidaException(String mensaje, Throwable cause) {
        super(ErrorMessage.PUNTORED_RESPUESTA_INVALIDA.getCodigo(), mensaje, cause);
    }
}
