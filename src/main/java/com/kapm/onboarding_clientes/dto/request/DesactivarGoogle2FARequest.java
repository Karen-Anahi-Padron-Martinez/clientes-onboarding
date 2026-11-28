package com.kapm.onboarding_clientes.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DesactivarGoogle2FARequest {

    @NotBlank(message = "La contraseña actual es obligatoria")
    private String passwordActual;

    @NotBlank(message = "El código de Google Authenticator es obligatorio")
    @Pattern(regexp = "^\\d{6}$", message = "El código debe tener 6 dígitos")
    private String codigo;
}
