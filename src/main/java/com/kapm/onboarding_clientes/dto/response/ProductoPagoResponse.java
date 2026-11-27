package com.kapm.onboarding_clientes.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.kapm.onboarding_clientes.config.JacksonConfig;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;

/**
 * Producto del catálogo local de PuntoRed tal como lo consume el frontend del cliente.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Producto o servicio disponible para pago o recarga")
public class ProductoPagoResponse {

    private Integer idProducto;
    private Integer idServicio;
    private String nombreProducto;
    private String nombreServicio;
    private Integer idCatTipoServicio;
    private Integer tipoFront;

    @Schema(description = "RECARGA (tipoFront 0) o SERVICIO (tipoFront 1 y 2)")
    private String tipoOperacion;

    @Schema(description = "Categoría configurada; si no existe, se usa 'Recargas' o 'Servicios'")
    private String categoria;

    private String descripcion;
    private String logoUrl;

    /** Imagen del recibo que señala dónde está la referencia (solo pagos de servicio; puede no existir). */
    private String imagenAyudaUrl;

    @Schema(description = "Expresión regular que debe cumplir la referencia o el teléfono (opcional)")
    private String regexReferencia;

    @JsonSerialize(using = JacksonConfig.BigDecimalTwoDecimalsSerializer.class)
    private BigDecimal montoMinimo;

    @JsonSerialize(using = JacksonConfig.BigDecimalTwoDecimalsSerializer.class)
    private BigDecimal montoMaximo;

    @Schema(description = "true cuando montoMinimo == montoMaximo (paquetes de monto fijo)")
    private boolean montoFijo;
}
