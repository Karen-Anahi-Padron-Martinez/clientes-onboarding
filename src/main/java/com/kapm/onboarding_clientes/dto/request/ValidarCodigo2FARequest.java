package com.kapm.onboarding_clientes.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Solicitud para validar y consumir un código 2FA ingresado por el usuario.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"codigo"})
@Schema(description = "Datos para validar el código de verificación 2FA de una operación")
public class ValidarCodigo2FARequest {

    @NotBlank(message = "El número de cuenta es obligatorio")
    @Schema(description = "Número de cuenta bancaria del cliente", example = "4819204812")
    private String numeroCuenta;

    @Schema(description = "CURP del cliente (opcional)", example = "MEHC920824HDFRMN09")
    private String curp;

    @NotBlank(message = "El código de verificación es obligatorio")
    @Pattern(regexp = "^\\d{6}$", message = "El código de verificación debe constar de exactamente 6 dígitos numéricos")
    @Schema(description = "Código de 6 dígitos numéricos enviado por correo", example = "492015")
    private String codigo;

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
