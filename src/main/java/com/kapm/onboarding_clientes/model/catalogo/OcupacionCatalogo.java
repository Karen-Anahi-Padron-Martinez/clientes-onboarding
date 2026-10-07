package com.kapm.onboarding_clientes.model.catalogo;

import lombok.Getter;
import lombok.RequiredArgsConstructor;


@Getter
@RequiredArgsConstructor
public enum OcupacionCatalogo {
    EMPLEADO_SECTOR_PRIVADO("EMPLEADO_SECTOR_PRIVADO", "Empleado Sector Privado"),
    EMPLEADO_SECTOR_PUBLICO("EMPLEADO_SECTOR_PUBLICO", "Servidor Público / Sector Público"),
    PROFESIONISTA_INDEPENDIENTE("PROFESIONISTA_INDEPENDIENTE", "Profesionista Independiente / Honorarios"),
    DESARROLLADOR_TI("DESARROLLADOR_TI", "Desarrollador / Tecnologías de la Información"),
    EMPRESARIO("EMPRESARIO", "Empresario / Dueño de Negocio"),
    COMERCIANTE("COMERCIANTE", "Comerciante"),
    ESTUDIANTE("ESTUDIANTE", "Estudiante"),
    JUBILADO_PENSIONADO("JUBILADO_PENSIONADO", "Jubilado / Pensionado"),
    DEDICADO_AL_HOGAR("DEDICADO_AL_HOGAR", "Dedicado(a) al Hogar"),
    OTRO("OTRO", "Otro oficio o profesión");

    private final String codigo;
    private final String descripcion;

    public static boolean esValido(String valor) {
        if (valor == null || valor.isBlank()) return false;
        String v = valor.trim().toUpperCase();
        for (OcupacionCatalogo o : values()) {
            if (o.name().equalsIgnoreCase(v) || o.codigo.equalsIgnoreCase(v) || o.descripcion.equalsIgnoreCase(valor.trim())) {
                return true;
            }
        }
        return false;
    }
}
