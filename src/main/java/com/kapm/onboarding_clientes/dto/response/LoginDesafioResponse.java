package com.kapm.onboarding_clientes.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;

/** Primer paso del login: la contraseña fue correcta y se requiere el código de Google Authenticator. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Respuesta del primer paso del login (requiere código de Google Authenticator)")
public class LoginDesafioResponse {

    private boolean requiereCodigo;
    private String username;
    @Schema(example = "GOOGLE_AUTHENTICATOR", description = "Método 2FA requerido: GOOGLE_AUTHENTICATOR")
    private String metodo2fa;
    @Schema(description = "True si es la primera vez que el usuario vincula Google Authenticator y debe escanear el QR")
    private Boolean requiereConfiguracion2fa;
    private String secreto2fa;
    private String otpauthUri2fa;
    private String qrCodeDataUrl;
    @Schema(example = "c***@email.com")
    private String correoEnmascarado;
    private LocalDateTime codigoExpiraEn;
    private String mensaje;
}
