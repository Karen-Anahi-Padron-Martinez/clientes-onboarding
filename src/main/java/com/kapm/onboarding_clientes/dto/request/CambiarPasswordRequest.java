package com.kapm.onboarding_clientes.dto.request;

import com.kapm.onboarding_clientes.validation.PatronesTexto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"passwordActual", "nuevaPassword"})
@Schema(description = "Cambio de contraseña desde el perfil del cliente autenticado")
public class CambiarPasswordRequest {

    @NotBlank(message = "La contraseña actual es obligatoria")
    private String passwordActual;

    @NotBlank(message = "La nueva contraseña es obligatoria")
    @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres")
    @Pattern(regexp = PatronesTexto.PASSWORD, message = PatronesTexto.MENSAJE_PASSWORD)
    private String nuevaPassword;
}
