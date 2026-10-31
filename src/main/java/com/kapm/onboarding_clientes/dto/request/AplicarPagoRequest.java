package com.kapm.onboarding_clientes.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Solicitud para aplicar y liquidar un pago de servicio o recarga TAE.
 * Incluye el código 2FA recibido por correo y una llave de idempotencia única.
 * Identificación 100% mediante Request Body (CURP o RFC + numeroCuenta, sin IDs de BD ni PathVariables).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"codigo"})
@Schema(description = "Solicitud para aplicar un pago o recarga con código 2FA e idempotencyKey (sin PathVariable)")
public class AplicarPagoRequest {

    @Pattern(regexp = "^[A-Z]{4}\\d{6}[HM][A-Z]{5}[A-Z0-9]{2}$", message = "El formato de la CURP es inválido (18 caracteres oficiales)")
    @Schema(description = "CURP del cliente titular (18 caracteres)", example = "MEHC920824HDFRMN09")
    private String curp;

    @Pattern(regexp = "^[A-Z&Ñ]{3,4}\\d{6}[A-Z0-9]{3}$", message = "El formato del RFC es inválido (12 o 13 caracteres)")
    @Schema(description = "RFC del cliente titular (12 o 13 caracteres)", example = "MEHC9208243A8")
    private String rfc;

    @NotBlank(message = "El número de cuenta es obligatorio")
    @Pattern(regexp = "^\\d{10}$", message = "El número de cuenta debe constar de 10 dígitos numéricos")
    @Schema(description = "Número único de cuenta bancaria", example = "4819204812")
    private String numeroCuenta;

    @NotNull(message = "El idProducto es obligatorio")
    @Positive(message = "El idProducto debe ser un número positivo")
    @Schema(description = "Identificador del producto en el catálogo oficial PuntoRed", example = "55")
    private Integer idProducto;

    @NotNull(message = "El idServicio es obligatorio")
    @Positive(message = "El idServicio debe ser un número positivo")
    @Schema(description = "Identificador del servicio en el catálogo oficial PuntoRed", example = "2")
    private Integer idServicio;

    @Size(max = 20, message = "El teléfono no debe exceder 20 caracteres")
    @Schema(description = "Número telefónico (obligatorio de 10 dígitos para recargas TAE; opcional para servicios)", example = "5512345678")
    private String telefono;

    @Size(max = 100, message = "La referencia no debe exceder 100 caracteres")
    @Schema(description = "Referencia del recibo o factura (obligatoria de 1 a 100 caracteres para pago de servicios)", example = "REC-CFE-001")
    private String referencia;

    @NotNull(message = "El monto es obligatorio")
    @DecimalMin(value = "0.01", message = "El monto debe ser mayor a cero")
    @Digits(integer = 13, fraction = 2, message = "El monto no puede tener más de 2 decimales")
    @Schema(description = "Monto de la transacción", example = "150.00")
    private BigDecimal monto;

    @NotBlank(message = "El código de verificación 2FA es obligatorio")
    @Pattern(regexp = "^\\d{6}$", message = "El código de verificación debe constar de exactamente 6 dígitos numéricos")
    @Schema(description = "Código numérico de 6 dígitos enviado por correo", example = "492015")
    private String codigo;

    @NotBlank(message = "La llave de idempotencia (idempotencyKey) es obligatoria")
    @Size(max = 100, message = "La llave de idempotencia no debe exceder 100 caracteres")
    @Schema(description = "Identificador único de la solicitud para prevenir cobros duplicados", example = "TX-8921-AF3")
    private String idempotencyKey;
}
