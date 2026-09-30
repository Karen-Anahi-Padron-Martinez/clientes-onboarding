package com.kapm.onboarding_clientes.dto.request;

import com.kapm.onboarding_clientes.model.TipoBiometria;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DatosSeguridadDTO {

    @NotBlank(message = "El nombre de usuario es obligatorio")
    @Size(min = 4, max = 50, message = "El usuario debe tener entre 4 y 50 caracteres")
    private String username;

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
    private String password;

    @NotNull(message = "El tipo de biometría es obligatorio")
    private TipoBiometria tipoBiometria;

    @NotBlank(message = "Los datos biométricos (patrón/plantilla) son obligatorios")
    private String datosBiometricos;
}
