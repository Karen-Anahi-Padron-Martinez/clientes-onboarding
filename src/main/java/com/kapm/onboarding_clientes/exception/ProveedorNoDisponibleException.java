package com.kapm.onboarding_clientes.exception;

/**
 * El proveedor externo no pudo atender la solicitud ANTES de enviar la transacción
 * (p. ej. falla al obtener el token). Garantiza que no se reservó ni se cobró saldo,
 * por lo que el cliente puede reintentar con seguridad.
 */
public class ProveedorNoDisponibleException extends RuntimeException {

    public ProveedorNoDisponibleException(Throwable cause) {
        super(ErrorMessage.PROVEEDOR_NO_DISPONIBLE.getMensaje(), cause);
    }

    public int getCodigo() {
        return ErrorMessage.PROVEEDOR_NO_DISPONIBLE.getCodigo();
    }
}
