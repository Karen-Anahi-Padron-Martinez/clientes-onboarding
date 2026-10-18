package com.kapm.onboarding_clientes.exception;

import lombok.Getter;

@Getter
public class RecursoDuplicadoException extends RuntimeException {

    private final ErrorMessage errorMessage;

    public RecursoDuplicadoException(ErrorMessage errorMessage) {
        super(errorMessage.getMensaje());
        this.errorMessage = errorMessage;
    }

    public RecursoDuplicadoException(ErrorMessage errorMessage, String detalle) {
        super(detalle != null && !detalle.isBlank() ? errorMessage.getMensaje() + ": " + detalle : errorMessage.getMensaje());
        this.errorMessage = errorMessage;
    }

    public RecursoDuplicadoException(String mensaje) {
        super(mensaje);
        this.errorMessage = ErrorMessage.RECURSO_DUPLICADO;
    }

    public int getCodigo() {
        return errorMessage != null ? errorMessage.getCodigo() : ErrorMessage.RECURSO_DUPLICADO.getCodigo();
    }
}
