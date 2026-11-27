package com.kapm.onboarding_clientes.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

/**
 * Resultado de validar una referencia (servicios) o un teléfono (recargas) contra el catálogo local.
 * PuntoRed no expone una consulta de adeudo, por lo que no se devuelven titular ni importe del recibo.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Resultado de la validación de referencia")
public class ValidacionReferenciaResponse {

    private boolean valida;
    private String mensaje;
    private String referencia;
    private String telefono;
    private ProductoPagoResponse producto;
}
