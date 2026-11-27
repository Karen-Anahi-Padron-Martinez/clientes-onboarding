package com.kapm.onboarding_clientes.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/** Datos de domicilio que se pueden completar a partir del código postal (Catálogo Nacional de Códigos Postales). */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Estado, municipio, país y colonias de un código postal")
public class CodigoPostalResponse {

    @Schema(example = "06600")
    private String codigoPostal;

    @Schema(description = "Código del catálogo de estados", example = "CIUDAD_DE_MEXICO")
    private String estado;

    @Schema(example = "Cuauhtémoc")
    private String municipio;

    @Schema(description = "Código del catálogo de países", example = "MEXICO")
    private String pais;

    @Schema(description = "Colonias o asentamientos del código postal", example = "[\"Juárez\"]")
    private List<String> colonias;
}
