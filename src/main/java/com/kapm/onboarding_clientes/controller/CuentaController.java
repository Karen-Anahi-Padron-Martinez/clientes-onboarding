package com.kapm.onboarding_clientes.controller;

import com.kapm.onboarding_clientes.dto.request.CuentaConsultaRequest;
import com.kapm.onboarding_clientes.dto.response.ApiResponse;
import com.kapm.onboarding_clientes.dto.response.CuentaResponse;
import com.kapm.onboarding_clientes.dto.response.SaldoResponse;
import com.kapm.onboarding_clientes.service.CuentaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cuentas")
@RequiredArgsConstructor
@Tag(name = "Cuentas Bancarias", description = "Endpoints para la consulta y saldo de cuentas bancarias (100% mediante Request Body)")
public class CuentaController {

    private final CuentaService cuentaService;

    @GetMapping("/activas")
    @Operation(summary = "Consultar cuentas activas", description = "Obtiene el listado de todas las cuentas bancarias en estatus ACTIVA.")
    public ResponseEntity<ApiResponse<List<CuentaResponse>>> obtenerCuentasActivas() {
        List<CuentaResponse> cuentas = cuentaService.obtenerCuentasActivas();
        return ResponseEntity.ok(ApiResponse.exito("Cuentas activas consultadas exitosamente", cuentas));
    }

    @PostMapping("/activas")
    @Operation(summary = "Consultar cuentas activas (POST)", description = "Obtiene el listado de todas las cuentas bancarias en estatus ACTIVA mediante POST.")
    public ResponseEntity<ApiResponse<List<CuentaResponse>>> obtenerCuentasActivasPost() {
        List<CuentaResponse> cuentas = cuentaService.obtenerCuentasActivas();
        return ResponseEntity.ok(ApiResponse.exito("Cuentas activas consultadas exitosamente", cuentas));
    }

    @PostMapping("/detalle")
    @Operation(summary = "Consultar cuenta por número (Request Body)", description = "Obtiene la información detallada de una cuenta bancaria a partir de su número único enviado en el Request Body. Cero PathVariable.")
    public ResponseEntity<ApiResponse<CuentaResponse>> obtenerCuentaPorNumero(
            @Valid @RequestBody CuentaConsultaRequest request) {
        CuentaResponse cuenta = cuentaService.obtenerCuentaPorNumero(request.getNumeroCuenta());
        return ResponseEntity.ok(ApiResponse.exito("Cuenta encontrada", cuenta));
    }

    @PostMapping("/saldo")
    @Operation(summary = "Consultar saldo de una cuenta (Request Body)", description = "Consulta el saldo disponible y estatus actual de una cuenta bancaria enviando el número de cuenta en el Request Body. Cero PathVariable.")
    public ResponseEntity<ApiResponse<SaldoResponse>> obtenerSaldoCuenta(
            @Valid @RequestBody CuentaConsultaRequest request) {
        SaldoResponse saldo = cuentaService.obtenerSaldoCuenta(request.getNumeroCuenta());
        return ResponseEntity.ok(ApiResponse.exito("Consulta de saldo realizada exitosamente", saldo));
    }
}
