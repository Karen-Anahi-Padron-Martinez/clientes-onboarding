package com.kapm.onboarding_clientes.service;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/**
 * Generador de códigos UPC (Unique Payment Code) para PuntoRed / GestoPago.
 * <p>
 * Reglas de negocio:
 * <ul>
 *   <li>Longitud máxima: 15 caracteres alfanuméricos.</li>
 *   <li>Debe generarse UNA SOLA VEZ por transacción de pago.</li>
 *   <li>Se almacena en PagoServicio con índice único.</li>
 *   <li>Se reutiliza idéntico en cualquier reenvío: así PuntoRed lo detecta como duplicado (CODIGO 06) y no cobra dos veces.</li>
 * </ul>
 */
@Component
public class UpcGenerator {

    private static final String ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * Genera un código UPC único de longitud fija de 13 caracteres (máx. 15):
     * 8 caracteres del timestamp actual en Base36 (mayúsculas) + 5 caracteres aleatorios criptográficamente seguros.
     *
     * @return Código alfanumérico en mayúsculas de 13 caracteres (ej. "LS32K918A4F7Z")
     */
    public String generar() {
        String timePart = Long.toString(System.currentTimeMillis(), 36).toUpperCase();

        StringBuilder randomPart = new StringBuilder(5);
        for (int i = 0; i < 5; i++) {
            randomPart.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }

        String upc = timePart + randomPart;
        if (upc.length() > 15) {
            upc = upc.substring(upc.length() - 15);
        }
        return upc;
    }

    /**
     * Valida si un código UPC cumple con el formato oficial (entre 3 y 15 caracteres alfanuméricos).
     *
     * @param upc Código a validar
     * @return true si cumple las restricciones, false en caso contrario
     */
    public boolean esValido(String upc) {
        return upc != null && upc.matches("^[A-Z0-9]{3,15}$");
    }
}
