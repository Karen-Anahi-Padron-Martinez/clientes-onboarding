package com.kapm.onboarding_clientes.controller;

import com.kapm.onboarding_clientes.dto.request.ActivarGoogle2FARequest;
import com.kapm.onboarding_clientes.dto.request.ActualizarPerfilRequest;
import com.kapm.onboarding_clientes.dto.request.CambiarPasswordRequest;
import com.kapm.onboarding_clientes.dto.request.DesactivarGoogle2FARequest;
import com.kapm.onboarding_clientes.dto.response.ApiResponse;
import com.kapm.onboarding_clientes.dto.response.ConfigurarGoogle2FAResponse;
import com.kapm.onboarding_clientes.dto.response.PerfilResponse;
import com.kapm.onboarding_clientes.dto.response.SaldoResponse;
import com.kapm.onboarding_clientes.service.Google2FAService;
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
    private final Google2FAService google2FAService;

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

    @PostMapping("/2fa/iniciar")
    @Operation(summary = "Iniciar vinculación de Google Authenticator", description = "Genera secreto TOTP y código QR en PNG para escanear en Google Authenticator. No activa el 2FA hasta confirmar un código en /api/perfil/2fa/activar.")
    public ResponseEntity<ApiResponse<ConfigurarGoogle2FAResponse>> iniciarGoogle2FA(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(ApiResponse.exito("Escanea el código QR en Google Authenticator e ingresa el código de 6 dígitos",
                google2FAService.iniciarConfiguracion(jwt.getSubject())));
    }

    @PostMapping("/2fa/activar")
    @Operation(summary = "Activar Google Authenticator", description = "Valida el primer código de 6 dígitos generado por Google Authenticator y activa la autenticación de dos factores.")
    public ResponseEntity<ApiResponse<Void>> activarGoogle2FA(@Valid @RequestBody ActivarGoogle2FARequest request,
                                                              @AuthenticationPrincipal Jwt jwt) {
        google2FAService.activar(jwt.getSubject(), request.getCodigo());
        return ResponseEntity.ok(ApiResponse.exito("Google Authenticator activado exitosamente", null));
    }

    @PostMapping("/2fa/desactivar")
    @Operation(summary = "Desactivar Google Authenticator", description = "Requiere contraseña actual y el código vigente de Google Authenticator. Al desactivarlo, el 2FA vuelve a correo electrónico.")
    public ResponseEntity<ApiResponse<Void>> desactivarGoogle2FA(@Valid @RequestBody DesactivarGoogle2FARequest request,
                                                                 @AuthenticationPrincipal Jwt jwt) {
        google2FAService.desactivar(jwt.getSubject(), request.getPasswordActual(), request.getCodigo());
        return ResponseEntity.ok(ApiResponse.exito("Google Authenticator desactivado exitosamente", null));
    }
}
