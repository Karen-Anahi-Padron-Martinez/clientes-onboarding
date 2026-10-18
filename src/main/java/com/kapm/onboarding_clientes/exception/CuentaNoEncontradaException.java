package com.kapm.onboarding_clientes.exception;

public class CuentaNoEncontradaException extends RecursoNoEncontradoException {

    public CuentaNoEncontradaException() {
        super(ErrorMessage.CUENTA_NO_ENCONTRADA);
    }

    public CuentaNoEncontradaException(ErrorMessage errorMessage) {
        super(errorMessage);
    }

    public CuentaNoEncontradaException(ErrorMessage errorMessage, String detalle) {
        super(errorMessage, detalle);
    }

    public CuentaNoEncontradaException(String mensaje) {
        super(ErrorMessage.CUENTA_NO_ENCONTRADA, mensaje);
    }

    public CuentaNoEncontradaException(Long id) {
        super(ErrorMessage.CUENTA_NO_ENCONTRADA, String.valueOf(id));
    }
}
