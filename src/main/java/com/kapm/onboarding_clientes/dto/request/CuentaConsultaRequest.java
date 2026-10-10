package com.kapm.onboarding_clientes.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request para consulta de cuenta bancaria y saldo mediante Request Body (sin PathVariable)")
public class CuentaConsultaRequest {

    @NotBlank(message = "El número de cuenta es obligatorio en el Request Body")
    @Schema(description = "Número único de cuenta bancaria asignado (10 dígitos)", example = "4819204812")
    private String numeroCuenta;
}
