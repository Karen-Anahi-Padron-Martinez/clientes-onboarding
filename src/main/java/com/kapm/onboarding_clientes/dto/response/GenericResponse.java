package com.kapm.onboarding_clientes.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

/**
 * Respuesta genérica estándar para errores y notificaciones de la API.
 * Muestra ÚNICAMENTE dos atributos ('codigo' y 'mensaje') según el estándar estricto requerido.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Respuesta estándar de error con únicamente código numérico y mensaje descriptivo")
public class GenericResponse {

    @Schema(description = "Código numérico de error o estatus", example = "1")
    private Integer codigo;

    @Schema(description = "Descripción o detalle amigable del error", example = "Datos no válidos")
    private String mensaje;
}
