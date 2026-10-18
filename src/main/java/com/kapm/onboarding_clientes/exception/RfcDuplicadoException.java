package com.kapm.onboarding_clientes.exception;

public class RfcDuplicadoException extends RecursoDuplicadoException {

    public RfcDuplicadoException() {
        super(ErrorMessage.RFC_DUPLICADO);
    }

    public RfcDuplicadoException(ErrorMessage errorMessage) {
        super(errorMessage);
    }

    public RfcDuplicadoException(ErrorMessage errorMessage, String rfc) {
        super(errorMessage, rfc);
    }

    public RfcDuplicadoException(String rfc) {
        super(ErrorMessage.RFC_DUPLICADO, rfc);
    }
}
