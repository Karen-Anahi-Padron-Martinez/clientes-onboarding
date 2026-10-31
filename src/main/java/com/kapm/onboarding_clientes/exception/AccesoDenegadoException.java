package com.kapm.onboarding_clientes.exception;

/**
 * El usuario autenticado intenta operar sobre un cliente o cuenta que no le pertenece.
 * Se responde 403 sin indicar si el recurso existe.
 */
public class AccesoDenegadoException extends RuntimeException {

    public AccesoDenegadoException() {
        super(ErrorMessage.OPERACION_NO_AUTORIZADA.getMensaje());
    }

    public int getCodigo() {
        return ErrorMessage.OPERACION_NO_AUTORIZADA.getCodigo();
    }
}
