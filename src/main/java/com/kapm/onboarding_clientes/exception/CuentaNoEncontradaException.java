package com.kapm.onboarding_clientes.exception;

public class CuentaNoEncontradaException extends RecursoNoEncontradoException {
    public CuentaNoEncontradaException(String mensaje) {
        super(mensaje);
    }

    public CuentaNoEncontradaException(Long id) {
        super("No se encontró la cuenta con el ID: " + id);
    }
}
