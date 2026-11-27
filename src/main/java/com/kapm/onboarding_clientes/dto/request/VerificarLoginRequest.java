package com.kapm.onboarding_clientes.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "codigo")
@Schema(description = "Segundo paso del login: código enviado al correo")
public class VerificarLoginRequest {

    @NotBlank(message = "El nombre de usuario es obligatorio")
    @Schema(example = "carlos_mendoza")
    private String username;

    @NotBlank(message = "El código de verificación es obligatorio")
    @Pattern(regexp = "^\\d{6}$", message = "El código de verificación debe tener 6 dígitos")
    @Schema(example = "492015")
    private String codigo;
}
