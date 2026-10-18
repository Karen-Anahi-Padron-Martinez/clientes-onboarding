package com.kapm.onboarding_clientes.dto.request;

import com.kapm.onboarding_clientes.entity.TipoBiometria;
import com.kapm.onboarding_clientes.validation.ValidationMessages;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginRequest {

    @NotBlank(message = ValidationMessages.SEGURIDAD_USERNAME_OBLIGATORIO)
    private String username;

    private String password;

    private TipoBiometria tipoBiometria;

    private String datosBiometricos;
}
