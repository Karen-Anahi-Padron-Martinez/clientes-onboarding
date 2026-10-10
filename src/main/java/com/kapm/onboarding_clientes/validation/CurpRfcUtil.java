package com.kapm.onboarding_clientes.validation;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;

public class CurpRfcUtil {

    /**
     * Expresión Regular oficial RENAPO para CURP (18 caracteres):
     * - 4 letras iniciales
     * - 6 dígitos para fecha (AAMMDD)
     * - 1 letra de sexo (H o M)
     * - 5 letras para entidad federativa y consonantes internas
     * - 2 caracteres alfanuméricos para homoclave y dígito verificador
     */
    public static final String CURP_REGEX = "^[A-Z]{4}\\d{6}[HM][A-Z]{5}[A-Z0-9]{2}$";

    /**
     * Expresión Regular oficial SAT para RFC de Personas Físicas (13 caracteres) y Morales (12 caracteres):
     * - 3 o 4 letras (incluyendo &, Ñ)
     * - 6 dígitos para fecha (AAMMDD)
     * - 3 caracteres alfanuméricos para homoclave
     */
    public static final String RFC_REGEX = "^[A-Z&Ñ]{3,4}\\d{6}[A-Z0-9]{3}$";

    private static final Pattern CURP_PATTERN = Pattern.compile(CURP_REGEX);
    private static final Pattern RFC_PATTERN = Pattern.compile(RFC_REGEX);

    /**
     * Valida si una cadena cumple con el formato oficial de CURP.
     *
     * @param curp Cadena a evaluar
     * @return true si es válida, false en caso contrario
     */
    public static boolean esCurpValida(String curp) {
        if (curp == null || curp.trim().length() != 18) {
            return false;
        }
        return CURP_PATTERN.matcher(curp.trim().toUpperCase()).matches();
    }

    /**
     * Valida si una cadena cumple con el formato oficial de RFC.
     *
     * @param rfc Cadena a evaluar
     * @return true si es válida, false en caso contrario
     */
    public static boolean esRfcValido(String rfc) {
        if (rfc == null) {
            return false;
        }
        String clean = rfc.trim().toUpperCase();
        if (clean.length() < 12 || clean.length() > 13) {
            return false;
        }
        return RFC_PATTERN.matcher(clean).matches();
    }

    /**
     * Limpia y estandariza una CURP (recorta espacios y convierte a mayúsculas).
     */
    public static String sanitizarCurp(String curp) {
        return curp != null ? curp.trim().toUpperCase() : null;
    }

    /**
     * Limpia y estandariza un RFC (recorta espacios y convierte a mayúsculas).
     */
    public static String sanitizarRfc(String rfc) {
        return rfc != null ? rfc.trim().toUpperCase() : null;
    }

    /**
     * Valida la coherencia básica entre la CURP y la fecha de nacimiento.
     * Los dígitos en posiciones 4 a 9 (índice 0) deben coincidir con AAMMDD.
     */
    public static boolean coincideFechaConCurp(String curp, LocalDate fechaNacimiento) {
        if (!esCurpValida(curp) || fechaNacimiento == null) {
            return false;
        }
        String curpClean = sanitizarCurp(curp);
        String fechaCurp = curpClean.substring(4, 10);
        String fechaEsperada = fechaNacimiento.format(DateTimeFormatter.ofPattern("yyMMdd"));
        return fechaCurp.equals(fechaEsperada);
    }
}
