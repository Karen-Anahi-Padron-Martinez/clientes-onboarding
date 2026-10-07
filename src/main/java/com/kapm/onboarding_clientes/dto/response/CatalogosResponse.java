package com.kapm.onboarding_clientes.dto.response;

import lombok.*;

import java.util.List;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CatalogosResponse {
    private List<CatalogoItemDTO> sexos;
    private List<CatalogoItemDTO> estadosCiviles;
    private List<CatalogoItemDTO> nacionalidades;
    private List<CatalogoItemDTO> ocupaciones;
    private List<CatalogoItemDTO> estadosRepublica;
    private List<CatalogoItemDTO> paises;
    private List<CatalogoItemDTO> tiposBiometria;
    private List<CatalogoItemDTO> estatusCuenta;
}
