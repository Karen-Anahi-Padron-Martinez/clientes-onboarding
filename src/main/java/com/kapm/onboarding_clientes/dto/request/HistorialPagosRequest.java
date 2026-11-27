package com.kapm.onboarding_clientes.dto.request;

import com.kapm.onboarding_clientes.entity.EstatusPago;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.*;

import java.time.LocalDate;

/**
 * Filtros del historial de pagos. El titular se toma del JWT, nunca del body.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Filtros y paginación del historial de pagos del titular")
public class HistorialPagosRequest {

    private EstatusPago estatus;

    @Schema(example = "2026-01-01")
    private LocalDate fechaDesde;

    @Schema(example = "2026-12-31")
    private LocalDate fechaHasta;

    @Min(value = 0, message = "La página no puede ser negativa")
    private Integer pagina;

    @Min(value = 1, message = "El tamaño de página debe ser al menos 1")
    @Max(value = 50, message = "El tamaño de página no puede ser mayor a 50")
    private Integer tamanio;
}
