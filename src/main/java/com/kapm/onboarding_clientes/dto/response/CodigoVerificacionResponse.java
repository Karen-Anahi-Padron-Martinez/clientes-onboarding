package com.kapm.onboarding_clientes.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Respuesta devuelta tras solicitar la emisión de un código 2FA.
 * Por razones de seguridad, NUNCA contiene el código en texto plano.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Respuesta de emisión de código de verificación 2FA")
public class CodigoVerificacionResponse {

    @Schema(description = "Indica si el correo fue despachado exitosamente hacia Brevo", example = "true")
    private boolean enviado;

    @Schema(description = "Dirección de correo electrónico enmascarada del receptor", example = "j***@dominio.com")
    private String correoEnmascarado;

    @Schema(description = "Fecha y hora exacta en la que expira el código", example = "2026-10-23T19:25:00")
    private LocalDateTime expiraEn;

    @Schema(description = "Mensaje informativo para el usuario", example = "Código de verificación enviado al correo registrado")
    private String mensaje;
}
