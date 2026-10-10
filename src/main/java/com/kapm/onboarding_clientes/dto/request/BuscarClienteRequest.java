package com.kapm.onboarding_clientes.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Criterios de búsqueda de cliente mediante Request Body")
public class BuscarClienteRequest {

    @Schema(description = "Buscar cliente por CURP", example = "MEHC920824HDFRMN09")
    private String curp;

    @Schema(description = "Buscar cliente por RFC", example = "MEHC9208243A8")
    private String rfc;

    @Schema(description = "Buscar cliente por Correo Electrónico", example = "carlos.mendoza@email.com")
    private String correo;

    @Schema(description = "Buscar cliente por Número de Cuenta Bancaria", example = "4819204812")
    private String numeroCuenta;

    @Schema(description = "Buscar cliente por ID primario (opcional)", example = "1")
    private Long id;
}
