package com.kapm.onboarding_clientes.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Identificador del cliente para operaciones por Request Body")
public class ClienteIdentificadorRequest {

    @Schema(description = "Clave CURP del cliente (18 caracteres oficiales RENAPO)", example = "MEHC920824HDFRMN09")
    private String curp;

    @Schema(description = "Clave RFC del cliente (12 o 13 caracteres oficiales SAT)", example = "MEHC9208243A8")
    private String rfc;

    @Schema(description = "Identificador numérico de base de datos (opcional)", example = "1")
    private Long id;
}
