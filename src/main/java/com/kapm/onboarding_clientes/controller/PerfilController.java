package com.kapm.onboarding_clientes.controller;

import com.kapm.onboarding_clientes.dto.request.ActualizarPerfilRequest;
import com.kapm.onboarding_clientes.dto.request.CambiarPasswordRequest;
import com.kapm.onboarding_clientes.dto.response.ApiResponse;
import com.kapm.onboarding_clientes.dto.response.PerfilResponse;
import com.kapm.onboarding_clientes.dto.response.SaldoResponse;
import com.kapm.onboarding_clientes.service.PerfilClienteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Perfil, saldo y contraseña del titular del token. El body nunca decide de quién son los datos. */
@RestController
@RequestMapping("/api/perfil")
@RequiredArgsConstructor
@Tag(name = "Perfil del cliente", description = "Datos, saldo y cambio de contraseña del cliente autenticado (Bearer JWT)")
@SecurityRequirement(name = "bearerAuth")
public class PerfilController {

    private final PerfilClienteService perfilClienteService;

    @PostMapping
    @Operation(summary = "Perfil del titular", description = "Datos personales, de contacto, domicilio y cuenta del dueño del token.")
    public ResponseEntity<ApiResponse<PerfilResponse>> perfil(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(ApiResponse.exito("Perfil consultado exitosamente",
                perfilClienteService.obtenerPerfil(jwt.getSubject())));
    }

    @PostMapping("/actualizar")
    @Operation(summary = "Actualizar perfil", description = "Modifica datos personales, de contacto, domicilio y laborales del titular. CURP y RFC no se pueden modificar (enviarlos responde 400). Cambiar el correo exige la contraseña actual.")
    public ResponseEntity<ApiResponse<PerfilResponse>> actualizar(@Valid @RequestBody ActualizarPerfilRequest request,
                                                                  @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(ApiResponse.exito("Perfil actualizado exitosamente",
                perfilClienteService.actualizarPerfil(jwt.getSubject(), request)));
    }

    @PostMapping("/saldo")
    @Operation(summary = "Saldo de la cuenta del titular", description = "Saldo disponible y contable de la cuenta del dueño del token.")
    public ResponseEntity<ApiResponse<SaldoResponse>> saldo(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(ApiResponse.exito("Consulta de saldo realizada exitosamente",
                perfilClienteService.obtenerSaldo(jwt.getSubject())));
    }

    @PostMapping("/password")
    @Operation(summary = "Cambiar contraseña", description = "Requiere la contraseña actual. La sesión en curso sigue activa.")
    public ResponseEntity<ApiResponse<Void>> cambiarPassword(@Valid @RequestBody CambiarPasswordRequest request,
                                                             @AuthenticationPrincipal Jwt jwt) {
        perfilClienteService.cambiarPassword(jwt.getSubject(), request);
        return ResponseEntity.ok(ApiResponse.exito("Contraseña actualizada exitosamente", null));
    }
}
