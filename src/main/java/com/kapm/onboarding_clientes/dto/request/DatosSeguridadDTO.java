package com.kapm.onboarding_clientes.dto.request;

import com.kapm.onboarding_clientes.entity.TipoBiometria;
import com.kapm.onboarding_clientes.validation.ValidationMessages;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Datos de credenciales de usuario y biometría para onboarding")
public class DatosSeguridadDTO {

    @NotBlank(message = ValidationMessages.SEGURIDAD_USERNAME_OBLIGATORIO)
    @Size(min = 4, max = 50, message = ValidationMessages.SEGURIDAD_USERNAME_SIZE)
    @Schema(description = "Nombre de usuario para acceso", example = "carlos_mendoza")
    private String username;

    @NotBlank(message = ValidationMessages.SEGURIDAD_PASSWORD_OBLIGATORIO)
    @Size(min = 8, message = ValidationMessages.SEGURIDAD_PASSWORD_SIZE)
    @Schema(description = "Contraseña de acceso (mínimo 8 caracteres)", example = "PasswordSeguro2026*")
    private String password;

    @NotNull(message = ValidationMessages.SEGURIDAD_TIPO_BIO_OBLIGATORIO)
    @Schema(description = "Modalidad biométrica", example = "HUELLA_DACTILAR")
    private TipoBiometria tipoBiometria;

    @NotBlank(message = ValidationMessages.SEGURIDAD_DATOS_BIO_OBLIGATORIO)
    @Schema(description = "Cadena o plantilla biométrica en crudo para hash SHA-256", example = "FINGERPRINT-PATTERN-DATA-SAMPLE-99")
    private String datosBiometricos;
}
