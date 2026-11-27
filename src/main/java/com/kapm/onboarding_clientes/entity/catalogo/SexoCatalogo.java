package com.kapm.onboarding_clientes.entity.catalogo;

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
}
