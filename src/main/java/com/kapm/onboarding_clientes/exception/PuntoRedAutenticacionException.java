package com.kapm.onboarding_clientes.exception;

/**
 * PuntoRed rechazó las credenciales o el token (success=false, HTTP 401/403 tras renovar).
 * <p>
 * La petición no llegó a procesarse, pero esto NO dice nada sobre una transacción anterior con el mismo UPC:
 * la verificación automática nunca libera saldo por este motivo.
 */
public class PuntoRedAutenticacionException extends PuntoRedRechazoException {

    public PuntoRedAutenticacionException(String mensaje, String codigoPuntoRed, String textoPuntoRed) {
        super(mensaje, codigoPuntoRed, textoPuntoRed);
    }
}
