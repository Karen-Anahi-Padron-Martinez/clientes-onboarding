package com.kapm.onboarding_clientes.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Texto de respuesta de PuntoRed apto para mostrarse al cliente. Algunos rechazos traen volcados técnicos
 * (mapas con la IP del servidor, dispositivo, lista de IPs permitidas…) que no deben salir de la API:
 * se conserva solo el mensaje legible y, si no hay ninguno, se usa una descripción genérica.
 */
public final class MensajeProveedor {

    private static final int LONGITUD_MAXIMA = 120;
    private static final Pattern MENSAJE_EN_VOLCADO = Pattern.compile("(?i)\\bmessage=([^,}\\]]*)");

    private MensajeProveedor() {
    }

    public static String limpiar(String texto) {
        if (texto == null || texto.isBlank()) {
            return texto;
        }
        String valor = texto.trim();
        // Los volcados de PuntoRed traen su propio mensaje en "message=..." (p. ej. "message=Origen Valido")
        Matcher mensaje = MENSAJE_EN_VOLCADO.matcher(valor);
        if (mensaje.find() && !mensaje.group(1).isBlank()) {
            String propio = mensaje.group(1).trim();
            return propio.length() > LONGITUD_MAXIMA ? propio.substring(0, LONGITUD_MAXIMA).trim() + "…" : propio;
        }
        String minusculas = valor.toLowerCase();
        if (minusculas.contains("validips") || minusculas.contains("ip=")) {
            return "Operación no autorizada por el proveedor: el servidor o dispositivo no está registrado";
        }

        // Todo lo que sigue a un volcado técnico ({...}, [...], (...) o clave=valor) se descarta
        int corte = valor.length();
        for (char c : new char[]{'{', '[', '('}) {
            int i = valor.indexOf(c);
            if (i >= 0 && i < corte) {
                corte = i;
            }
        }
        int igual = valor.indexOf('=');
        if (igual >= 0 && igual < corte) {
            int inicioClave = valor.lastIndexOf(' ', igual);
            corte = Math.max(0, inicioClave);
        }
        String legible = valor.substring(0, corte).replaceAll("[\\s:,·-]+$", "").trim();
        if (legible.isEmpty()) {
            return "Operación rechazada por el proveedor";
        }
        return legible.length() > LONGITUD_MAXIMA ? legible.substring(0, LONGITUD_MAXIMA).trim() + "…" : legible;
    }
}
