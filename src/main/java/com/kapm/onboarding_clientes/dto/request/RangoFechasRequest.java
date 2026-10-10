package com.kapm.onboarding_clientes.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Rango de fechas de registro mediante Request Body")
public class RangoFechasRequest {

    @NotNull(message = "La fecha de inicio es obligatoria")
    @Schema(description = "Fecha de inicio (Formato YYYY-MM-DD)", example = "2026-01-01")
    private LocalDate fechaInicio;

    @NotNull(message = "La fecha de fin es obligatoria")
    @Schema(description = "Fecha de fin (Formato YYYY-MM-DD)", example = "2026-12-31")
    private LocalDate fechaFin;
}
