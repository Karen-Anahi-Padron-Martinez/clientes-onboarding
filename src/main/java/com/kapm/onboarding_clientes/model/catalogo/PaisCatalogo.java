package com.kapm.onboarding_clientes.model.catalogo;

import lombok.Getter;
import lombok.RequiredArgsConstructor;


@Getter
@RequiredArgsConstructor
public enum PaisCatalogo {
    MEXICO("MEXICO", "México"),
    ESTADOS_UNIDOS("ESTADOS_UNIDOS", "Estados Unidos"),
    CANADA("CANADA", "Canadá"),
    ESPANYA("ESPANYA", "España"),
    COLOMBIA("COLOMBIA", "Colombia"),
    ARGENTINA("ARGENTINA", "Argentina"),
    OTRO("OTRO", "Otro país");

    private final String codigo;
    private final String descripcion;

    public static boolean esValido(String valor) {
        if (valor == null || valor.isBlank()) return false;
        String v = valor.trim().toUpperCase()
                .replace("Á", "A")
                .replace("É", "E")
                .replace("Í", "I")
                .replace("Ó", "O")
                .replace("Ú", "U")
                .replace(" ", "_");

        for (PaisCatalogo p : values()) {
            String normName = p.name().replace("Á", "A").replace("É", "E").replace("Í", "I").replace("Ó", "O").replace("Ú", "U");
            if (normName.equalsIgnoreCase(v) || p.codigo.equalsIgnoreCase(v) || p.descripcion.equalsIgnoreCase(valor.trim())) {
                return true;
            }
        }
        return false;
    }
}
