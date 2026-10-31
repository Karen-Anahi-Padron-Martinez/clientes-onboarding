package com.kapm.onboarding_clientes.controller;

import com.kapm.onboarding_clientes.dto.request.LoginRequest;
import com.kapm.onboarding_clientes.dto.response.ApiResponse;
import com.kapm.onboarding_clientes.dto.response.LoginResponse;
import com.kapm.onboarding_clientes.service.ClienteService;
import com.kapm.onboarding_clientes.service.SeguridadBiometriaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticación y Biometría", description = "Endpoints para la validación de credenciales cifradas, firmas biométricas y control de sesión con temporizador de inactividad (100% mediante Request Body)")
public class AuthController {

    private final ClienteService clienteService;
    private final SeguridadBiometriaService seguridadBiometriaService;

    @PostMapping("/login")
    @Operation(summary = "Autenticación segura (Request Body con Password Cifrada o Biometría)", description = "Permite autenticar a un usuario registrado mediante su contraseña cifrada con BCrypt o su patrón biométrico encriptado en SHA-256 en el Request Body. Activa la bandera 'loggeado = true' e inicia el contador de actividad (Límite: 5s).")
    public ResponseEntity<ApiResponse<LoginResponse>> autenticar(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = clienteService.autenticarCliente(request);
        return ResponseEntity.ok(ApiResponse.exito(response.getMensaje(), response));
    }

    @PostMapping("/estado")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Consultar estado de la sesión propia (Bearer JWT)", description = "Retorna el valor 'loggeado' (true/false) del usuario dueño del token y el tiempo desde su última actividad. Si supera el límite de inactividad, la sesión pasa a false y el token deja de ser válido.")
    public ResponseEntity<ApiResponse<LoginResponse>> consultarEstadoSesion(@AuthenticationPrincipal Jwt jwt) {
        LoginResponse response = seguridadBiometriaService.consultarEstadoSesion(jwt.getSubject());
        return ResponseEntity.ok(ApiResponse.exito(response.getMensaje(), response));
    }

    @PostMapping("/logout")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Cerrar la sesión propia (Bearer JWT)", description = "Marca 'loggeado = false' para el dueño del token e invalida el token aunque no haya expirado.")
    public ResponseEntity<ApiResponse<Void>> logout(@AuthenticationPrincipal Jwt jwt) {
        seguridadBiometriaService.cerrarSesion(jwt.getSubject());
        return ResponseEntity.ok(ApiResponse.exito("Sesión cerrada exitosamente", null));
    }
}
