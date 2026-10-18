package com.kapm.onboarding_clientes.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.kapm.onboarding_clientes.validation.ValidationMessages;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Pattern;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Identificador oficial del cliente para operaciones mediante Request Body (CURP o RFC, sin ID de BD)")
public class ClienteIdentificadorRequest {

    @Pattern(regexp = "^$|^[a-zA-Z]{4}\\d{6}[hHmM][a-zA-Z]{5}[a-zA-Z0-9]{2}$", message = ValidationMessages.CLIENTE_CURP_PATTERN)
    @Schema(description = "Clave CURP del cliente (18 caracteres oficiales RENAPO)", example = "MEHC920824HDFRMN09")
    private String curp;

    @Pattern(regexp = "^$|^[a-zA-Z&ñÑ]{3,4}\\d{6}[a-zA-Z0-9]{3}$", message = ValidationMessages.CLIENTE_RFC_PATTERN)
    @Schema(description = "Clave RFC del cliente (12 o 13 caracteres oficiales SAT)", example = "MEHC9208243A8")
    private String rfc;

    @JsonIgnore
    @AssertTrue(message = ValidationMessages.VALIDAR_ID_PRESENTE)
    public boolean isIdentificadorPresente() {
        return (curp != null && !curp.isBlank()) || (rfc != null && !rfc.isBlank());
    }
}
