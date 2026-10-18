package com.kapm.onboarding_clientes.exception;

public class CorreoDuplicadoException extends RecursoDuplicadoException {

    public CorreoDuplicadoException() {
        super(ErrorMessage.CORREO_DUPLICADO);
    }

    public CorreoDuplicadoException(ErrorMessage errorMessage) {
        super(errorMessage);
    }

    public CorreoDuplicadoException(ErrorMessage errorMessage, String correo) {
        super(errorMessage, correo);
    }

    public CorreoDuplicadoException(String correo) {
        super(ErrorMessage.CORREO_DUPLICADO, correo);
    }
}
