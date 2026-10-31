package com.kapm.onboarding_clientes.exception;

/**
 * PuntoRed respondió CODIGO 06 ("Transacción aplicada anteriormente").
 * <p>
 * Su significado depende del contexto:
 * <ul>
 *   <li>Primer envío de un UPC nuevo: el proveedor detectó un duplicado de OTRA operación (mismo servicio y monto
 *   en 24 h). Esta operación NO se cobró, por lo que se trata como rechazo (de ahí que herede de
 *   {@link PuntoRedRechazoException}).</li>
 *   <li>Reenvío del mismo UPC tras un resultado desconocido: la operación original SÍ se aplicó; la verificación
 *   automática lo trata como éxito.</li>
 * </ul>
 */
public class PuntoRedTransaccionDuplicadaException extends PuntoRedRechazoException {

    public static final String CODIGO = "06";

    public PuntoRedTransaccionDuplicadaException(String textoPuntoRed) {
        super(CODIGO, textoPuntoRed);
    }
}
