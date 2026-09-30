package com.kapm.onboarding_clientes.controller;

import com.kapm.onboarding_clientes.dto.response.ApiResponse;
import com.kapm.onboarding_clientes.dto.response.CuentaResponse;
import com.kapm.onboarding_clientes.dto.response.SaldoResponse;
import com.kapm.onboarding_clientes.service.CuentaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cuentas")
@RequiredArgsConstructor
@Tag(name = "Cuentas Bancarias", description = "Endpoints para la consulta y saldo de cuentas bancarias")
public class CuentaController {

    private final CuentaService cuentaService;

    @GetMapping("/activas")
    @Operation(summary = "Consultar cuentas activas", description = "Obtiene el listado de todas las cuentas bancarias en estatus ACTIVA.")
    public ResponseEntity<ApiResponse<List<CuentaResponse>>> obtenerCuentasActivas() {
        List<CuentaResponse> cuentas = cuentaService.obtenerCuentasActivas();
        return ResponseEntity.ok(ApiResponse.exito("Cuentas activas consultadas exitosamente", cuentas));
    }

    @GetMapping("/{numeroCuenta}")
    @Operation(summary = "Consultar cuenta por número", description = "Obtiene la información detallada de una cuenta bancaria a partir de su número único de 10 dígitos.")
    public ResponseEntity<ApiResponse<CuentaResponse>> obtenerCuentaPorNumero(@PathVariable String numeroCuenta) {
        CuentaResponse cuenta = cuentaService.obtenerCuentaPorNumero(numeroCuenta);
        return ResponseEntity.ok(ApiResponse.exito("Cuenta encontrada", cuenta));
    }

    @GetMapping("/{numeroCuenta}/saldo")
    @Operation(summary = "Consultar saldo de una cuenta", description = "Consulta el saldo disponible y estatus actual de una cuenta bancaria por su número de cuenta.")
    public ResponseEntity<ApiResponse<SaldoResponse>> obtenerSaldoCuenta(@PathVariable String numeroCuenta) {
        SaldoResponse saldo = cuentaService.obtenerSaldoCuenta(numeroCuenta);
        return ResponseEntity.ok(ApiResponse.exito("Consulta de saldo realizada exitosamente", saldo));
    }
}
