package com.kapm.onboarding_clientes.model.catalogo;

import lombok.Getter;
import lombok.RequiredArgsConstructor;


@Getter
@RequiredArgsConstructor
public enum SexoCatalogo {
    MASCULINO("MASCULINO", "Masculino"),
    FEMENINO("FEMENINO", "Femenino"),
    OTRO("OTRO", "Otro / No binario");

    private final String codigo;
    private final String descripcion;

    public static boolean esValido(String valor) {
        if (valor == null || valor.isBlank()) return false;
        String v = valor.trim().toUpperCase();
        for (SexoCatalogo s : values()) {
            if (s.name().equalsIgnoreCase(v) || s.codigo.equalsIgnoreCase(v) || s.descripcion.equalsIgnoreCase(v)) {
                return true;
            }
        }
        return false;
    }
}
