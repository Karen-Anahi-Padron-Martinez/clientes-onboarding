package com.kapm.onboarding_clientes.util;

/**
 * Enmascaramiento de datos sensibles para bitácoras: nunca se registra un identificador financiero completo.
 */
public final class DatosSensibles {

    private DatosSensibles() {
    }

    /** Conserva solo los últimos 4 caracteres (p. ej. ******1234). */
    public static String enmascarar(String valor) {
        return valor == null || valor.length() < 4 ? "****" : "******" + valor.substring(valor.length() - 4);
    }
}
