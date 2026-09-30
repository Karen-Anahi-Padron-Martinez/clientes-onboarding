package com.kapm.onboarding_clientes.exception;

import java.util.Map;

public class ErrorValidacionException extends RuntimeException {

    private final Map<String, String> erroresValidacion;

    public ErrorValidacionException(String mensaje, Map<String, String> erroresValidacion) {
        super(mensaje);
        this.erroresValidacion = erroresValidacion;
    }

    public Map<String, String> getErroresValidacion() {
        return erroresValidacion;
    }
}
