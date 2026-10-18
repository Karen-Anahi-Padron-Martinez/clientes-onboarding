package com.kapm.onboarding_clientes.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.kapm.onboarding_clientes.validation.ValidationMessages;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;

/**
 * Petición para actualizar información editable identificando al cliente por CURP en el Request Body.
 * Excluye los 7 campos inmutables de identidad (nombre, apellidos, fecha de nacimiento, curp, rfc, nacionalidad).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = false)
@Schema(description = "Petición para actualizar información de cliente por CURP (excluye campos inmutables)")
public class ActualizarPorCurpRequest {

    @NotBlank(message = ValidationMessages.CLIENTE_CURP_OBLIGATORIO)
    @Pattern(regexp = "^[a-zA-Z]{4}\\d{6}[hHmM][a-zA-Z]{5}[a-zA-Z0-9]{2}$", message = ValidationMessages.CLIENTE_CURP_PATTERN)
    @Schema(description = "CURP identificadora del cliente a actualizar (solo búsqueda, inmutable)", example = "MEHC920824HDFRMN09", requiredMode = Schema.RequiredMode.REQUIRED)
    private String curp;

    @NotBlank(message = ValidationMessages.CLIENTE_SEXO_OBLIGATORIO)
    @Schema(description = "Sexo (MASCULINO, FEMENINO, OTRO)", example = "MASCULINO")
    private String sexo;

    @NotBlank(message = ValidationMessages.CLIENTE_ESTADO_CIVIL_OBLIGATORIO)
    @Schema(description = "Estado civil (SOLTERO, CASADO, DIVORCIADO, VIUDO, UNION_LIBRE)", example = "CASADO")
    private String estadoCivil;

    @NotBlank(message = ValidationMessages.CLIENTE_CORREO_OBLIGATORIO)
    @Email(message = ValidationMessages.CLIENTE_CORREO_EMAIL)
    @Size(max = 100, message = ValidationMessages.CLIENTE_CORREO_SIZE)
    @Schema(description = "Correo electrónico de contacto", example = "carlos.actualizado@email.com")
    private String correo;

    @NotBlank(message = ValidationMessages.CLIENTE_TELEFONO_MOVIL_OBLIGATORIO)
    @Pattern(regexp = "^\\d{10}$", message = ValidationMessages.CLIENTE_TELEFONO_MOVIL_PATTERN)
    @Schema(description = "Teléfono móvil a 10 dígitos", example = "5598765432")
    private String telefonoMovil;

    @Pattern(regexp = "^(\\d{10})?$", message = ValidationMessages.CLIENTE_TELEFONO_ALT_PATTERN)
    @Schema(description = "Teléfono alternativo opcional a 10 dígitos", example = "5512344321")
    private String telefonoAlternativo;

    @NotNull(message = ValidationMessages.CLIENTE_DOMICILIO_OBLIGATORIO)
    @Valid
    private DomicilioDTO domicilio;

    @NotNull(message = ValidationMessages.CLIENTE_LABORAL_OBLIGATORIO)
    @Valid
    private DatosLaboralesDTO laboral;
}
