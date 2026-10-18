package com.kapm.onboarding_clientes.exception;

import lombok.Getter;

import java.util.Map;

@Getter
public class ErrorValidacionException extends RuntimeException {

    private final ErrorMessage errorMessage;
    private final Map<String, String> erroresValidacion;

    public ErrorValidacionException(ErrorMessage errorMessage, Map<String, String> erroresValidacion) {
        super(errorMessage.getMensaje());
        this.errorMessage = errorMessage;
        this.erroresValidacion = erroresValidacion;
    }

    public ErrorValidacionException(String mensaje, Map<String, String> erroresValidacion) {
        super(mensaje);
        this.errorMessage = ErrorMessage.VALIDACION_FALLIDA;
        this.erroresValidacion = erroresValidacion;
    }

    public int getCodigo() {
        return errorMessage != null ? errorMessage.getCodigo() : ErrorMessage.VALIDACION_FALLIDA.getCodigo();
    }
}
