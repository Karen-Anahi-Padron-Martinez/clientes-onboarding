package com.kapm.onboarding_clientes.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Validación previa de la referencia o teléfono de un producto")
public class ValidarReferenciaRequest {

    @NotNull(message = "El idProducto es obligatorio")
    @Positive(message = "El idProducto debe ser un número positivo")
    private Integer idProducto;

    @NotNull(message = "El idServicio es obligatorio")
    @Positive(message = "El idServicio debe ser un número positivo")
    private Integer idServicio;

    @Size(max = 100, message = "La referencia no debe exceder 100 caracteres")
    private String referencia;

    @Size(max = 20, message = "El teléfono no debe exceder 20 caracteres")
    private String telefono;
}
