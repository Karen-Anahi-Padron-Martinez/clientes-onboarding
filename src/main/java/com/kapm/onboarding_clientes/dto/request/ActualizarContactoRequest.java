package com.kapm.onboarding_clientes.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.kapm.onboarding_clientes.validation.ValidationMessages;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = false)
@Schema(description = "Solicitud de actualización de contacto mediante Request Body")
public class ActualizarContactoRequest {

    @Pattern(regexp = "^$|^[a-zA-Z]{4}\\d{6}[hHmM][a-zA-Z]{5}[a-zA-Z0-9]{2}$", message = ValidationMessages.CLIENTE_CURP_PATTERN)
    @Schema(description = "CURP identificadora del cliente", example = "MEHC920824HDFRMN09")
    private String curp;

    @Pattern(regexp = "^$|^[a-zA-Z&ñÑ]{3,4}\\d{6}[a-zA-Z0-9]{3}$", message = ValidationMessages.CLIENTE_RFC_PATTERN)
    @Schema(description = "RFC identificador del cliente", example = "MEHC9208243A8")
    private String rfc;

    @NotBlank(message = ValidationMessages.CLIENTE_CORREO_OBLIGATORIO)
    @Email(message = ValidationMessages.CLIENTE_CORREO_EMAIL)
    @Size(max = 100, message = ValidationMessages.CLIENTE_CORREO_SIZE)
    @Schema(description = "Correo electrónico nuevo", example = "nuevo.contacto@email.com")
    private String correo;

    @NotBlank(message = ValidationMessages.CLIENTE_TELEFONO_MOVIL_OBLIGATORIO)
    @Pattern(regexp = "^\\d{10}$", message = ValidationMessages.CLIENTE_TELEFONO_MOVIL_PATTERN)
    @Schema(description = "Teléfono celular a 10 dígitos", example = "5512345678")
    private String telefonoMovil;

    @Pattern(regexp = "^(\\d{10})?$", message = ValidationMessages.CLIENTE_TELEFONO_ALT_PATTERN)
    @Schema(description = "Teléfono alternativo opcional a 10 dígitos", example = "5587654321")
    private String telefonoAlternativo;

    @JsonIgnore
    @AssertTrue(message = ValidationMessages.VALIDAR_ID_PRESENTE)
    public boolean isIdentificadorPresente() {
        return (curp != null && !curp.isBlank()) || (rfc != null && !rfc.isBlank());
    }
}
