package com.kapm.onboarding_clientes.exception;

import lombok.Getter;

@Getter
public class ReglaNegocioException extends RuntimeException {

    private final ErrorMessage errorMessage;

    public ReglaNegocioException(ErrorMessage errorMessage) {
        super(errorMessage.getMensaje());
        this.errorMessage = errorMessage;
    }

    public ReglaNegocioException(ErrorMessage errorMessage, String detalle) {
        super(detalle != null && !detalle.isBlank() ? errorMessage.getMensaje() + ": " + detalle : errorMessage.getMensaje());
        this.errorMessage = errorMessage;
    }

    public ReglaNegocioException(String mensaje) {
        super(mensaje);
        this.errorMessage = ErrorMessage.REGLA_NEGOCIO;
    }

    public int getCodigo() {
        return errorMessage != null ? errorMessage.getCodigo() : ErrorMessage.REGLA_NEGOCIO.getCodigo();
    }
}
