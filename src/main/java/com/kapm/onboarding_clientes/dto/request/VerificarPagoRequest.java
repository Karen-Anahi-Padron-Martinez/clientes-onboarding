package com.kapm.onboarding_clientes.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * Solicitud de verificación manual con PuntoRed de un pago con resultado desconocido.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Verificación con el proveedor de un pago PENDIENTE o EN_CONCILIACION")
public class VerificarPagoRequest {

    @NotBlank(message = "El UPC es obligatorio")
    @Size(max = 15, message = "El UPC no debe exceder 15 caracteres")
    @Schema(description = "UPC enviado a PuntoRed en sendTx", example = "260128193045123")
    private String upc;
}
