package com.kapm.onboarding_clientes.entity.catalogo;

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
}
