package com.kapm.onboarding_clientes.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Datos para vincular Google Authenticator (TOTP)")
public class ConfigurarGoogle2FAResponse {

    private boolean habilitado;
    private String secreto;
    private String otpauthUri;
    private String qrCodeDataUrl;
    private String emisor;
    private String cuenta;
}
