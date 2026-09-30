package com.kapm.onboarding_clientes.exception;

public class ClienteNoEncontradoException extends RecursoNoEncontradoException {
    public ClienteNoEncontradoException(String mensaje) {
        super(mensaje);
    }

    public ClienteNoEncontradoException(Long id) {
        super("No se encontró el cliente con el ID: " + id);
    }
}
