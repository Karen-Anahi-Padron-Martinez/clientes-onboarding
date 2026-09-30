package com.kapm.onboarding_clientes.exception;

public class CurpDuplicadaException extends RecursoDuplicadoException {
    public CurpDuplicadaException(String curp) {
        super("Ya existe un cliente registrado con la CURP: " + curp);
    }
}
