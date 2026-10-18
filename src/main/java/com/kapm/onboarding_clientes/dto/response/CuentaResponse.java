package com.kapm.onboarding_clientes.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.kapm.onboarding_clientes.config.JacksonConfig;
import com.kapm.onboarding_clientes.entity.EstatusCuenta;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Detalle de cuenta bancaria del cliente")
public class CuentaResponse {

    @JsonIgnore
    @Schema(hidden = true)
    private Long id;

    @Schema(description = "Número único de cuenta bancaria (10 dígitos)", example = "4819204812")
    private String numeroCuenta;

    @JsonSerialize(using = JacksonConfig.BigDecimalTwoDecimalsSerializer.class)
    @Schema(description = "Saldo disponible", example = "3500.00")
    private BigDecimal saldo;

    @Schema(description = "Estatus actual de la cuenta bancaria", example = "ACTIVA")
    private EstatusCuenta estatus;

    @Schema(description = "Fecha de apertura de la cuenta")
    private LocalDateTime fechaCreacion;

    @Schema(description = "Nombre completo del titular", example = "Carlos Alberto Mendoza Hernández")
    private String clienteNombreCompleto;

    @Schema(description = "CURP del titular", example = "MEHC920824HDFRMN09")
    private String clienteCurp;
}
