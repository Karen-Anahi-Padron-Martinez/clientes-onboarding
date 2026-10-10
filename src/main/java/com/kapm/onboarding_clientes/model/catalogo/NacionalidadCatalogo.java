package com.kapm.onboarding_clientes.model.catalogo;

import lombok.Getter;
import lombok.RequiredArgsConstructor;


@Getter
@RequiredArgsConstructor
public enum NacionalidadCatalogo {
    MEXICANA("MEXICANA", "Mexicana"),
    ESTADOUNIDENSE("ESTADOUNIDENSE", "Estadounidense"),
    CANADIENSE("CANADIENSE", "Canadiense"),
    ESPANYOLA("ESPANYOLA", "Española"),
    COLOMBIANA("COLOMBIANA", "Colombiana"),
    ARGENTINA("ARGENTINA", "Argentina"),
    VENEZOLANA("VENEZOLANA", "Venezolana"),
    PERUANA("PERUANA", "Peruana"),
    CHILENA("CHILENA", "Chilena"),
    OTRA("OTRA", "Otra nacionalidad");

    private final String codigo;
    private final String descripcion;

    public static boolean esValido(String valor) {
        if (valor == null || valor.isBlank()) return false;
        String v = valor.trim().toUpperCase();
        for (NacionalidadCatalogo n : values()) {
            if (n.name().equalsIgnoreCase(v) || n.codigo.equalsIgnoreCase(v) || n.descripcion.equalsIgnoreCase(valor.trim())) {
                return true;
            }
        }
        return false;
    }
}
