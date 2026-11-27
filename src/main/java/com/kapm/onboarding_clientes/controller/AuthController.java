package com.kapm.onboarding_clientes.controller;

import com.kapm.onboarding_clientes.dto.request.LoginRequest;
import com.kapm.onboarding_clientes.dto.request.RestablecerPasswordRequest;
import com.kapm.onboarding_clientes.dto.request.SolicitarRecuperacionRequest;
import com.kapm.onboarding_clientes.dto.request.VerificarLoginRequest;
import com.kapm.onboarding_clientes.dto.response.ApiResponse;
import com.kapm.onboarding_clientes.dto.response.LoginDesafioResponse;
import com.kapm.onboarding_clientes.dto.response.LoginResponse;
import com.kapm.onboarding_clientes.service.AccesoClienteService;
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

    private final AccesoClienteService accesoClienteService;
    private final SeguridadBiometriaService seguridadBiometriaService;

    @PostMapping("/login")
    @Operation(summary = "Login paso 1: usuario y contraseña", description = "Valida las credenciales y envía un código de 6 dígitos al correo registrado. No emite token: hay que confirmar el código en /api/auth/login/verificar. Para reenviar el código se vuelve a llamar a este endpoint.")
    public ResponseEntity<ApiResponse<LoginDesafioResponse>> autenticar(@Valid @RequestBody LoginRequest request) {
        LoginDesafioResponse response = accesoClienteService.iniciarLogin(request);
        return ResponseEntity.ok(ApiResponse.exito(response.getMensaje(), response));
    }

    @PostMapping("/login/verificar")
    @Operation(summary = "Login paso 2: código enviado al correo", description = "Valida el código (un solo uso, 5 minutos, 3 intentos) y emite el JWT de sesión.")
    public ResponseEntity<ApiResponse<LoginResponse>> verificarLogin(@Valid @RequestBody VerificarLoginRequest request) {
        LoginResponse response = accesoClienteService.verificarLogin(request);
        return ResponseEntity.ok(ApiResponse.exito(response.getMensaje(), response));
    }

    @PostMapping("/recuperar/solicitar")
    @Operation(summary = "Recuperar contraseña: solicitar código", description = "Envía un código al correo registrado del usuario (por username o correo). Responde igual aunque el usuario no exista.")
    public ResponseEntity<ApiResponse<Void>> solicitarRecuperacion(@Valid @RequestBody SolicitarRecuperacionRequest request) {
        accesoClienteService.solicitarRecuperacion(request);
        return ResponseEntity.ok(ApiResponse.exito("Si los datos corresponden a una cuenta, enviamos un código a su correo registrado", null));
    }

    @PostMapping("/recuperar/restablecer")
    @Operation(summary = "Recuperar contraseña: restablecer", description = "Valida el código de recuperación, guarda la nueva contraseña y cierra la sesión abierta.")
    public ResponseEntity<ApiResponse<Void>> restablecerPassword(@Valid @RequestBody RestablecerPasswordRequest request) {
        accesoClienteService.restablecerPassword(request);
        return ResponseEntity.ok(ApiResponse.exito("Contraseña actualizada. Ya puedes iniciar sesión", null));
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
