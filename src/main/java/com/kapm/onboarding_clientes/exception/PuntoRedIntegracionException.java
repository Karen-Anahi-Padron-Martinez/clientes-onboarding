package com.kapm.onboarding_clientes.exception;

import lombok.Getter;

/**
 * Excepción base lanzada cuando ocurre un error en los mecanismos de integración,
 * comunicación, cifrado o procesamiento con el proveedor PuntoRed (GestoPago).
 * Conserva el código HTTP/negocio de la aplicación así como el CODIGO y TEXTO del proveedor.
 */
@Getter
public class PuntoRedIntegracionException extends RuntimeException {

    private final int codigo;
    private final String codigoPuntoRed;
    private final String textoPuntoRed;

    public PuntoRedIntegracionException(String mensaje) {
        this(ErrorMessage.ERROR_INTERNO.getCodigo(), mensaje, null, null, null);
    }

    public PuntoRedIntegracionException(String mensaje, Throwable cause) {
        this(ErrorMessage.ERROR_INTERNO.getCodigo(), mensaje, null, null, cause);
    }

    public PuntoRedIntegracionException(int codigo, String mensaje) {
        this(codigo, mensaje, null, null, null);
    }

    public PuntoRedIntegracionException(int codigo, String mensaje, Throwable cause) {
        this(codigo, mensaje, null, null, cause);
    }

    public PuntoRedIntegracionException(int codigo, String mensaje, String codigoPuntoRed, String textoPuntoRed) {
        this(codigo, mensaje, codigoPuntoRed, textoPuntoRed, null);
    }

    public PuntoRedIntegracionException(int codigo, String mensaje, String codigoPuntoRed, String textoPuntoRed, Throwable cause) {
        super(mensaje, cause);
        this.codigo = codigo;
        this.codigoPuntoRed = codigoPuntoRed;
        this.textoPuntoRed = textoPuntoRed;
    }
}
