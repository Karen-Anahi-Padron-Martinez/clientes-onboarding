package com.kapm.onboarding_clientes.exception;

public class ClienteYaRegistradoException extends RecursoDuplicadoException {
    public ClienteYaRegistradoException(String mensaje) {
        super(mensaje);
    }
}
