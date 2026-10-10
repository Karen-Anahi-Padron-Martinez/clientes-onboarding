package com.kapm.onboarding_clientes.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request para consulta de sesión y logout mediante Request Body (sin PathVariable)")
public class UsuarioSesionRequest {

    @NotBlank(message = "El nombre de usuario (username) es obligatorio en el Request Body")
    @Schema(description = "Nombre de usuario o username registrado", example = "carlos_mendoza")
    private String username;
}
