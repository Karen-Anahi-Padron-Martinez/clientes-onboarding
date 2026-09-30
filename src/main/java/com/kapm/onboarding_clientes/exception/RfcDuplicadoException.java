package com.kapm.onboarding_clientes.exception;

public class RfcDuplicadoException extends RecursoDuplicadoException {
    public RfcDuplicadoException(String rfc) {
        super("Ya existe un cliente registrado con el RFC: " + rfc);
    }
}
