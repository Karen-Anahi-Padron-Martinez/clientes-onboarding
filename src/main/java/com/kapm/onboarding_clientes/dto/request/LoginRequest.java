package com.kapm.onboarding_clientes.dto.request;

import com.kapm.onboarding_clientes.model.TipoBiometria;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginRequest {

    @NotBlank(message = "El nombre de usuario es obligatorio")
    private String username;

    private String password;

    private TipoBiometria tipoBiometria;

    private String datosBiometricos;
}
