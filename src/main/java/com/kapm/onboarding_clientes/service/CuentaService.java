package com.kapm.onboarding_clientes.service;

import com.kapm.onboarding_clientes.dto.response.CuentaResponse;
import com.kapm.onboarding_clientes.dto.response.SaldoResponse;
import com.kapm.onboarding_clientes.model.Cuenta;

import java.math.BigDecimal;
import java.util.List;

public interface CuentaService {

    Cuenta crearCuentaInicial(BigDecimal saldoInicial);

    String generarNumeroCuentaUnico();

    CuentaResponse obtenerCuentaPorNumero(String numeroCuenta);

    List<CuentaResponse> obtenerCuentasActivas();

    SaldoResponse obtenerSaldoCuenta(String numeroCuenta);
}
