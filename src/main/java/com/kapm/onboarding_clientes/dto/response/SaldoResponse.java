package com.kapm.onboarding_clientes.dto.response;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.kapm.onboarding_clientes.config.JacksonConfig;
import com.kapm.onboarding_clientes.model.EstatusCuenta;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SaldoResponse {

    private String numeroCuenta;
    @JsonSerialize(using = JacksonConfig.BigDecimalTwoDecimalsSerializer.class)
    private BigDecimal saldoDisponible;

    @JsonSerialize(using = JacksonConfig.BigDecimalTwoDecimalsSerializer.class)
    private BigDecimal saldoContable;
    private EstatusCuenta estatus;
    private String titularNombreCompleto;
    private String titularCurp;
    private Boolean usuarioLoggeado;
    private Long segundosInactividad;
    private LocalDateTime fechaConsulta;
}
