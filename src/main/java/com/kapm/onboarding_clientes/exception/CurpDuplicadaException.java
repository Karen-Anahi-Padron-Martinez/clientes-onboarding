package com.kapm.onboarding_clientes.exception;

public class CurpDuplicadaException extends RecursoDuplicadoException {

    public CurpDuplicadaException() {
        super(ErrorMessage.CURP_DUPLICADA);
    }

    public CurpDuplicadaException(ErrorMessage errorMessage) {
        super(errorMessage);
    }

    public CurpDuplicadaException(ErrorMessage errorMessage, String curp) {
        super(errorMessage, curp);
    }

    public CurpDuplicadaException(String curp) {
        super(ErrorMessage.CURP_DUPLICADA, curp);
    }
}
