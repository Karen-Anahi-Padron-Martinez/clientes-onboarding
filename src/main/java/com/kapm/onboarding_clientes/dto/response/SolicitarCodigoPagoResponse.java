package com.kapm.onboarding_clientes.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.kapm.onboarding_clientes.config.JacksonConfig;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Respuesta devuelta tras la emisión exitosa del código 2FA.
 * Incluye el correo enmascarado del titular registrado en BD.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Respuesta de emisión de código 2FA con correo enmascarado")
public class SolicitarCodigoPagoResponse {

    @Schema(description = "Mensaje informativo", example = "Código de verificación enviado al correo registrado")
    private String mensaje;

    @Schema(description = "Correo electrónico registrado del cliente, enmascarado", example = "c***@dominio.com")
    private String correoEnmascarado;

    @Schema(description = "Número de cuenta bancaria", example = "4819204812")
    private String numeroCuenta;

    @Schema(description = "Identificador del producto en el catálogo oficial PuntoRed", example = "55")
    private Integer idProducto;

    @Schema(description = "Identificador del servicio en el catálogo oficial PuntoRed", example = "2")
    private Integer idServicio;

    @JsonSerialize(using = JacksonConfig.BigDecimalTwoDecimalsSerializer.class)
    @Schema(description = "Monto de la operación", example = "150.00")
    private BigDecimal monto;

    @Schema(description = "Fecha y hora límite de expiración del código 2FA")
    private LocalDateTime expiraEn;
}
