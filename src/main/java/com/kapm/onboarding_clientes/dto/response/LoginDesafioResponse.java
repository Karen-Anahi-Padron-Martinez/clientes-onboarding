package com.kapm.onboarding_clientes.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;

/** Primer paso del login: la contraseña fue correcta y se envió el código al correo. Todavía no hay token. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Respuesta del primer paso del login (requiere el código enviado al correo)")
public class LoginDesafioResponse {

    private boolean requiereCodigo;
    private String username;
    @Schema(example = "GOOGLE_AUTHENTICATOR", description = "Método 2FA requerido: CORREO o GOOGLE_AUTHENTICATOR")
    private String metodo2fa;
    @Schema(example = "c***@email.com")
    private String correoEnmascarado;
    private LocalDateTime codigoExpiraEn;
    private String mensaje;
}
