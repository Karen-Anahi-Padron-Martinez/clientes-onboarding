package com.kapm.onboarding_clientes.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Solicitud para generar y enviar un código de verificación 2FA para autorizar un pago.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Datos de la transacción para emitir código de verificación 2FA")
public class EmitirCodigo2FARequest {

    @NotBlank(message = "El número de cuenta es obligatorio")
    @Schema(description = "Número de cuenta bancaria del cliente", example = "4819204812")
    private String numeroCuenta;

    @Schema(description = "CURP del cliente (opcional si se provee la cuenta)", example = "MEHC920824HDFRMN09")
    private String curp;

    @NotNull(message = "El idProducto es obligatorio")
    @Positive(message = "El idProducto debe ser un número positivo")
    @Schema(description = "Identificador del producto en el catálogo local", example = "55")
    private Integer idProducto;

    @NotNull(message = "El idServicio es obligatorio")
    @Positive(message = "El idServicio debe ser un número positivo")
    @Schema(description = "Identificador del servicio en el catálogo local", example = "2")
    private Integer idServicio;

    @Size(max = 20, message = "El teléfono no debe exceder 20 caracteres")
    @Schema(description = "Número telefónico asociado a la operación", example = "5512345678")
    private String telefono;

    @Size(max = 100, message = "La referencia no debe exceder 100 caracteres")
    @Schema(description = "Referencia del recibo o servicio", example = "REC-CFE-001")
    private String referencia;

    @NotNull(message = "El monto es obligatorio")
    @DecimalMin(value = "0.01", message = "El monto debe ser mayor a cero")
    @Digits(integer = 13, fraction = 2, message = "El monto no puede tener más de 2 decimales")
    @Schema(description = "Monto de la transacción", example = "150.00")
    private BigDecimal monto;
}
