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
@ToString(exclude = {"codigo", "nuevaPassword"})
@Schema(description = "Restablecimiento de contraseña con el código recibido por correo")
public class RestablecerPasswordRequest {

    @NotBlank(message = "Ingresa tu usuario o correo electrónico")
    @Size(max = 100, message = "El usuario o correo no puede exceder 100 caracteres")
    @Schema(example = "carlos_mendoza")
    private String identificador;

    @NotBlank(message = "El código de verificación es obligatorio")
    @Pattern(regexp = "^\\d{6}$", message = "El código de verificación debe tener 6 dígitos")
    @Schema(example = "492015")
    private String codigo;

    @NotBlank(message = "La nueva contraseña es obligatoria")
    @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres")
    @Pattern(regexp = PatronesTexto.PASSWORD, message = PatronesTexto.MENSAJE_PASSWORD)
    @Schema(example = "NuevaPassword2026*")
    private String nuevaPassword;
}
