package com.kapm.onboarding_clientes.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DomicilioDTO {

    @NotBlank(message = "La calle es obligatoria")
    @Size(max = 100, message = "La calle no debe exceder 100 caracteres")
    private String calle;

    @NotBlank(message = "El número exterior es obligatorio")
    @Size(max = 20, message = "El número exterior no debe exceder 20 caracteres")
    private String numeroExterior;

    @Size(max = 20, message = "El número interior no debe exceder 20 caracteres")
    private String numeroInterior;

    @NotBlank(message = "La colonia es obligatoria")
    @Size(max = 100, message = "La colonia no debe exceder 100 caracteres")
    private String colonia;

    @NotBlank(message = "El municipio o alcaldía es obligatorio")
    @Size(max = 100, message = "El municipio no debe exceder 100 caracteres")
    private String municipio;

    @NotBlank(message = "El estado es obligatorio")
    @Size(max = 100, message = "El estado no debe exceder 100 caracteres")
    private String estado;

    @NotBlank(message = "El código postal es obligatorio")
    @Pattern(regexp = "^\\d{5}$", message = "El código postal debe contener exactamente 5 dígitos")
    private String codigoPostal;

    @NotBlank(message = "El país es obligatorio")
    @Size(max = 100, message = "El país no debe exceder 100 caracteres")
    private String pais;
}
