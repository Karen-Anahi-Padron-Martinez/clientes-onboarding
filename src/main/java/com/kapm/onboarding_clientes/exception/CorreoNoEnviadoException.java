package com.kapm.onboarding_clientes.exception;

import lombok.Getter;

/**
 * Excepción lanzada cuando ocurre un error en el envío de correos transaccionales mediante Brevo
 * (fallo HTTP, 401 no autorizado, timeout de red o error de servidor).
 */
@Getter
public class CorreoNoEnviadoException extends RuntimeException {

    private final int codigo;

    public CorreoNoEnviadoException(String mensaje) {
        super(mensaje);
        this.codigo = ErrorMessage.CORREO_NO_ENVIADO.getCodigo();
    }

    public CorreoNoEnviadoException(String mensaje, Throwable cause) {
        super(mensaje, cause);
        this.codigo = ErrorMessage.CORREO_NO_ENVIADO.getCodigo();
    }

    public CorreoNoEnviadoException(ErrorMessage errorMessage, String detalle) {
        super(detalle != null && !detalle.isBlank() ? errorMessage.getMensaje() + ": " + detalle : errorMessage.getMensaje());
        this.codigo = errorMessage.getCodigo();
    }

    public CorreoNoEnviadoException(int codigo, String mensaje) {
        super(mensaje);
        this.codigo = codigo;
    }

    public CorreoNoEnviadoException(int codigo, String mensaje, Throwable cause) {
        super(mensaje, cause);
        this.codigo = codigo;
    }
}
