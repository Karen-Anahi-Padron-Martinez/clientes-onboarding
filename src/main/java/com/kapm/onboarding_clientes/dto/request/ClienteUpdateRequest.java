package com.kapm.onboarding_clientes.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.kapm.onboarding_clientes.validation.ValidationMessages;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;

/**
 * DTO para la actualización de cliente.
 * Los datos de identidad (nombre, segundoNombre, apellidos, fechaNacimiento, curp, rfc y nacionalidad)
 * NO son modificables bajo ninguna circunstancia.
 * CURP y RFC solo se permiten en el cuerpo como identificadores únicos de búsqueda.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = false)
@Schema(description = "Solicitud de actualización de cliente. Excluye datos de identidad inmutables.")
public class ClienteUpdateRequest {

    // Identificadores de búsqueda (Solo lectura / localización; NO modificables en BD)
    @Pattern(regexp = "^$|^[a-zA-Z]{4}\\d{6}[hHmM][a-zA-Z]{5}[a-zA-Z0-9]{2}$", message = ValidationMessages.CLIENTE_CURP_PATTERN)
    @Schema(description = "CURP del cliente para identificar el registro a actualizar (solo búsqueda, inmutable)", example = "MEHC920824HDFRMN09")
    private String curp;

    @Pattern(regexp = "^$|^[a-zA-Z&ñÑ]{3,4}\\d{6}[a-zA-Z0-9]{3}$", message = ValidationMessages.CLIENTE_RFC_PATTERN)
    @Schema(description = "RFC del cliente para identificar el registro a actualizar (solo búsqueda, inmutable)", example = "MEHC9208243A8")
    private String rfc;

    // Datos Modificables
    @NotBlank(message = ValidationMessages.CLIENTE_SEXO_OBLIGATORIO)
    @Schema(description = "Sexo (MASCULINO, FEMENINO, OTRO)", example = "MASCULINO")
    private String sexo;

    @NotBlank(message = ValidationMessages.CLIENTE_ESTADO_CIVIL_OBLIGATORIO)
    @Schema(description = "Estado civil (SOLTERO, CASADO, DIVORCIADO, etc.)", example = "CASADO")
    private String estadoCivil;

    @NotBlank(message = ValidationMessages.CLIENTE_CORREO_OBLIGATORIO)
    @Email(message = ValidationMessages.CLIENTE_CORREO_EMAIL)
    @Size(max = 100, message = ValidationMessages.CLIENTE_CORREO_SIZE)
    @Schema(description = "Correo electrónico actualizado", example = "carlos.actualizado@email.com")
    private String correo;

    @NotBlank(message = ValidationMessages.CLIENTE_TELEFONO_MOVIL_OBLIGATORIO)
    @Pattern(regexp = "^\\d{10}$", message = ValidationMessages.CLIENTE_TELEFONO_MOVIL_PATTERN)
    @Schema(description = "Teléfono móvil a 10 dígitos", example = "5598765432")
    private String telefonoMovil;

    @Pattern(regexp = "^(\\d{10})?$", message = ValidationMessages.CLIENTE_TELEFONO_ALT_PATTERN)
    @Schema(description = "Teléfono alternativo opcional a 10 dígitos", example = "5512344321")
    private String telefonoAlternativo;

    // Secciones complejas (Modificables)
    @NotNull(message = ValidationMessages.CLIENTE_DOMICILIO_OBLIGATORIO)
    @Valid
    private DomicilioDTO domicilio;

    @NotNull(message = ValidationMessages.CLIENTE_LABORAL_OBLIGATORIO)
    @Valid
    private DatosLaboralesDTO laboral;

    @JsonIgnore
    @AssertTrue(message = ValidationMessages.VALIDAR_ID_PRESENTE)
    public boolean isIdentificadorPresente() {
        return (curp != null && !curp.isBlank()) || (rfc != null && !rfc.isBlank());
    }
}
