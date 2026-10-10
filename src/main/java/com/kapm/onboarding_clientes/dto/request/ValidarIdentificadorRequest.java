package com.kapm.onboarding_clientes.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Solicitud para validación y disponibilidad mediante Request Body")
public class ValidarIdentificadorRequest {

    @Schema(description = "CURP a validar sintaxis y disponibilidad", example = "MEHC920824HDFRMN09")
    private String curp;

    @Schema(description = "RFC a validar sintaxis y disponibilidad", example = "MEHC9208243A8")
    private String rfc;
}
