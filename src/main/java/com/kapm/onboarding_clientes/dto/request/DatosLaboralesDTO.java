package com.kapm.onboarding_clientes.dto.request;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.kapm.onboarding_clientes.config.JacksonConfig;
import com.kapm.onboarding_clientes.validation.MontoDecimal;
import com.kapm.onboarding_clientes.validation.PatronesTexto;
import com.kapm.onboarding_clientes.validation.ValidationMessages;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Información laboral y económica del cliente")
public class DatosLaboralesDTO {

    @NotBlank(message = ValidationMessages.LABORAL_OCUPACION_OBLIGATORIO)
    @Size(max = 100, message = ValidationMessages.LABORAL_OCUPACION_SIZE)
    @Schema(description = "Ocupación o profesión", example = "Arquitecto de Software")
    private String ocupacion;

    @Size(max = 100, message = ValidationMessages.LABORAL_EMPRESA_SIZE)
    @Pattern(regexp = PatronesTexto.EMPRESA, message = PatronesTexto.MENSAJE_EMPRESA)
    @Schema(description = "Nombre de la empresa o centro laboral (opcional)", example = "Sistemas Financieros MX")
    private String empresa;

    @NotNull(message = ValidationMessages.LABORAL_INGRESO_OBLIGATORIO)
    @DecimalMin(value = "0.01", message = ValidationMessages.LABORAL_INGRESO_MIN)
    @DecimalMax(value = "9999999.99", message = "El ingreso mensual no puede exceder 9,999,999.99")
    @MontoDecimal(message = ValidationMessages.LABORAL_INGRESO_MONTO)
    @JsonSerialize(using = JacksonConfig.BigDecimalTwoDecimalsSerializer.class)
    @Schema(description = "Ingreso mensual comprobable (con 2 decimales)", example = "45000.00")
    private BigDecimal ingresoMensual;
}
