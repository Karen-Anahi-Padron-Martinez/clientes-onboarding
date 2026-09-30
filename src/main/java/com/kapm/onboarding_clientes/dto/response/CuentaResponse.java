package com.kapm.onboarding_clientes.dto.response;

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
    private BigDecimal saldo;
    private EstatusCuenta estatus;
    private LocalDateTime fechaCreacion;
    private String clienteNombreCompleto;
    private String clienteCurp;
}
