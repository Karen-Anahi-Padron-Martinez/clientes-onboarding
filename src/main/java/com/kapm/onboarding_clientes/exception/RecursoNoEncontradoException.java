package com.kapm.onboarding_clientes.exception;

import lombok.Getter;

@Getter
public class RecursoNoEncontradoException extends RuntimeException {

    private final ErrorMessage errorMessage;

    public RecursoNoEncontradoException(ErrorMessage errorMessage) {
        super(errorMessage.getMensaje());
        this.errorMessage = errorMessage;
    }

    public RecursoNoEncontradoException(ErrorMessage errorMessage, String detalle) {
        super(detalle != null && !detalle.isBlank() ? errorMessage.getMensaje() + ": " + detalle : errorMessage.getMensaje());
        this.errorMessage = errorMessage;
    }

    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
        this.errorMessage = ErrorMessage.RECURSO_NO_ENCONTRADO;
    }

    public int getCodigo() {
        return errorMessage != null ? errorMessage.getCodigo() : ErrorMessage.RECURSO_NO_ENCONTRADO.getCodigo();
    }
}
