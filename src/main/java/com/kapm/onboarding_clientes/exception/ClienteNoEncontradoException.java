package com.kapm.onboarding_clientes.exception;

public class ClienteNoEncontradoException extends RecursoNoEncontradoException {

    public ClienteNoEncontradoException() {
        super(ErrorMessage.CLIENTE_NO_ENCONTRADO);
    }

    public ClienteNoEncontradoException(ErrorMessage errorMessage) {
        super(errorMessage);
    }

    public ClienteNoEncontradoException(ErrorMessage errorMessage, String detalle) {
        super(errorMessage, detalle);
    }

    public ClienteNoEncontradoException(String mensaje) {
        super(ErrorMessage.CLIENTE_NO_ENCONTRADO, mensaje);
    }

    public ClienteNoEncontradoException(Long id) {
        super(ErrorMessage.CLIENTE_NO_ENCONTRADO, String.valueOf(id));
    }
}
