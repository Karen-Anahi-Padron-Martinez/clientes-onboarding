package com.kapm.onboarding_clientes.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * Resolución manual de un pago EN_CONCILIACION, tras confirmar con PuntoRed (portal, reporte o soporte)
 * si la transacción identificada por su UPC fue procesada.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Resolución manual de un pago con resultado desconocido")
public class ResolverConciliacionRequest {

    @NotBlank(message = "El UPC es obligatorio")
    @Size(max = 15, message = "El UPC no debe exceder 15 caracteres")
    @Schema(description = "UPC enviado a PuntoRed en sendTx", example = "260128193045123")
    private String upc;

    @NotNull(message = "Debe indicar si el proveedor procesó la transacción")
    @Schema(description = "true si PuntoRed confirmó la transacción; false si confirmó que NO la procesó")
    private Boolean procesadaPorProveedor;

    @Size(max = 50, message = "El número de autorización no debe exceder 50 caracteres")
    @Schema(description = "Número de autorización de PuntoRed (obligatorio si fue procesada)", example = "AUT-88192")
    private String numAutorizacion;

    @Size(max = 50, message = "El id de transacción no debe exceder 50 caracteres")
    private String idTxProveedor;

    @AssertTrue(message = "El número de autorización es obligatorio cuando la transacción fue procesada")
    @Schema(hidden = true)
    public boolean isAutorizacionPresenteSiProcesada() {
        return !Boolean.TRUE.equals(procesadaPorProveedor) || (numAutorizacion != null && !numAutorizacion.isBlank());
    }
}
