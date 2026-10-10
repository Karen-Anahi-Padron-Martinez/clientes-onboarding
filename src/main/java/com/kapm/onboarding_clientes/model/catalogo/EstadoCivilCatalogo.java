package com.kapm.onboarding_clientes.model.catalogo;

import lombok.Getter;
import lombok.RequiredArgsConstructor;


@Getter
@RequiredArgsConstructor
public enum EstadoCivilCatalogo {
    SOLTERO("SOLTERO", "Soltero / a"),
    CASADO("CASADO", "Casado / a"),
    DIVORCIADO("DIVORCIADO", "Divorciado / a"),
    VIUDO("VIUDO", "Viudo / a"),
    UNION_LIBRE("UNION_LIBRE", "Unión Libre");

    private final String codigo;
    private final String descripcion;

    public static boolean esValido(String valor) {
        if (valor == null || valor.isBlank()) return false;
        String v = valor.trim().toUpperCase().replace("/", "").replace(" ", "_");
        for (EstadoCivilCatalogo e : values()) {
            if (e.name().equalsIgnoreCase(v) || e.codigo.equalsIgnoreCase(v) || e.descripcion.equalsIgnoreCase(valor.trim())) {
                return true;
            }
        }
        return false;
    }
}
