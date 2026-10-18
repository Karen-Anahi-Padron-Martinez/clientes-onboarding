package com.kapm.onboarding_clientes.exception;

public class ClienteYaRegistradoException extends RecursoDuplicadoException {

    public ClienteYaRegistradoException() {
        super(ErrorMessage.CLIENTE_YA_REGISTRADO);
    }

    public ClienteYaRegistradoException(ErrorMessage errorMessage) {
        super(errorMessage);
    }

    public ClienteYaRegistradoException(ErrorMessage errorMessage, String detalle) {
        super(errorMessage, detalle);
    }

    public ClienteYaRegistradoException(String mensaje) {
        super(ErrorMessage.CLIENTE_YA_REGISTRADO, mensaje);
    }
}
