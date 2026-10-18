package com.kapm.onboarding_clientes.dto.request;

import com.kapm.onboarding_clientes.validation.ValidationMessages;
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
@Schema(description = "Datos estructurados de domicilio del cliente")
public class DomicilioDTO {

    @NotBlank(message = ValidationMessages.DOMICILIO_CALLE_OBLIGATORIO)
    @Size(max = 100, message = ValidationMessages.DOMICILIO_CALLE_SIZE)
    @Schema(description = "Nombre de la calle o avenida", example = "Av. Insurgentes Sur")
    private String calle;

    @NotBlank(message = ValidationMessages.DOMICILIO_NUM_EXT_OBLIGATORIO)
    @Size(max = 20, message = ValidationMessages.DOMICILIO_NUM_EXT_SIZE)
    @Schema(description = "Número exterior", example = "456")
    private String numeroExterior;

    @Size(max = 20, message = ValidationMessages.DOMICILIO_NUM_INT_SIZE)
    @Schema(description = "Número interior, piso o departamento (opcional)", example = "Piso 8-B")
    private String numeroInterior;

    @NotBlank(message = ValidationMessages.DOMICILIO_COLONIA_OBLIGATORIO)
    @Size(max = 100, message = ValidationMessages.DOMICILIO_COLONIA_SIZE)
    @Schema(description = "Colonia o asentamiento", example = "Roma Norte")
    private String colonia;

    @NotBlank(message = ValidationMessages.DOMICILIO_MUNICIPIO_OBLIGATORIO)
    @Size(max = 100, message = ValidationMessages.DOMICILIO_MUNICIPIO_SIZE)
    @Schema(description = "Municipio o alcaldía", example = "Cuauhtémoc")
    private String municipio;

    @NotBlank(message = ValidationMessages.DOMICILIO_ESTADO_OBLIGATORIO)
    @Size(max = 100, message = ValidationMessages.DOMICILIO_ESTADO_SIZE)
    @Schema(description = "Entidad Federativa (de las 32 oficiales)", example = "Ciudad de México")
    private String estado;

    @NotBlank(message = ValidationMessages.DOMICILIO_CP_OBLIGATORIO)
    @Pattern(regexp = "^\\d{5}$", message = ValidationMessages.DOMICILIO_CP_PATTERN)
    @Schema(description = "Código postal mexicano (exactamente 5 dígitos)", example = "06700")
    private String codigoPostal;

    @NotBlank(message = ValidationMessages.DOMICILIO_PAIS_OBLIGATORIO)
    @Size(max = 100, message = ValidationMessages.DOMICILIO_PAIS_SIZE)
    @Schema(description = "País de residencia", example = "México")
    private String pais;
}
