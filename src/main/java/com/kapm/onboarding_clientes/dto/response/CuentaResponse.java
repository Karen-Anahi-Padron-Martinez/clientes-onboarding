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
public class CuentaResponse {

    private Long id;
    private String numeroCuenta;
    @JsonSerialize(using = JacksonConfig.BigDecimalTwoDecimalsSerializer.class)
    private BigDecimal saldo;
    private EstatusCuenta estatus;
    private LocalDateTime fechaCreacion;
    private String clienteNombreCompleto;
    private String clienteCurp;
}
