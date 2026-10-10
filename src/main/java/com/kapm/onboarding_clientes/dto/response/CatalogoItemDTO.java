package com.kapm.onboarding_clientes.dto.response;

import lombok.*;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CatalogoItemDTO {
    private String codigo;
    private String descripcion;
}
