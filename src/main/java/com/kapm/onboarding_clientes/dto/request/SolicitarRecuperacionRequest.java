package com.kapm.onboarding_clientes.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Solicitud de código para restablecer la contraseña")
public class SolicitarRecuperacionRequest {

    @NotBlank(message = "Ingresa tu usuario o correo electrónico")
    @Size(max = 100, message = "El usuario o correo no puede exceder 100 caracteres")
    @Schema(description = "Nombre de usuario o correo registrado", example = "carlos_mendoza")
    private String identificador;
}
