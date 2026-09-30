package com.kapm.onboarding_clientes.exception;

public class CorreoDuplicadoException extends RecursoDuplicadoException {
    public CorreoDuplicadoException(String correo) {
        super("Ya existe un cliente registrado con el correo electrónico: " + correo);
    }
}
