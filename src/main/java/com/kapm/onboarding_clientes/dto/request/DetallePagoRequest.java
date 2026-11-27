package com.kapm.onboarding_clientes.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Consulta del detalle de una operación propia por su UPC (folio)")
public class DetallePagoRequest {

    @NotBlank(message = "El folio (upc) es obligatorio")
    @Pattern(regexp = "^[A-Za-z0-9]{1,30}$", message = "El folio (upc) solo admite letras y dígitos (máximo 30)")
    private String upc;
}
