package com.kapm.onboarding_clientes.controller;

import com.kapm.onboarding_clientes.dto.request.LoginRequest;
import com.kapm.onboarding_clientes.dto.request.UsuarioSesionRequest;
import com.kapm.onboarding_clientes.dto.response.ApiResponse;
import com.kapm.onboarding_clientes.dto.response.LoginResponse;
import com.kapm.onboarding_clientes.service.ClienteService;
import com.kapm.onboarding_clientes.service.SeguridadBiometriaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
    @Operation(summary = "Consultar estado de sesión y temporizador de inactividad 5s (Request Body)", description = "Retorna el valor 'loggeado' (true/false) del usuario especificando el tiempo transcurrido desde su última actividad. Recibe el 'username' en el Request Body. Si supera los 5 segundos de inactividad, la sesión pasa automáticamente a false. Cero PathVariable.")
    public ResponseEntity<ApiResponse<LoginResponse>> consultarEstadoSesion(@Valid @RequestBody UsuarioSesionRequest request) {
        LoginResponse response = seguridadBiometriaService.consultarEstadoSesion(request.getUsername());
        return ResponseEntity.ok(ApiResponse.exito(response.getMensaje(), response));
    }

    @PostMapping("/logout")
    @Operation(summary = "Cerrar Sesión de Usuario (Request Body)", description = "Cambia manualmente el estado 'loggeado' a false para el usuario especificado en el Request Body. Cero PathVariable.")
    public ResponseEntity<ApiResponse<Void>> logout(@Valid @RequestBody UsuarioSesionRequest request) {
        seguridadBiometriaService.cerrarSesion(request.getUsername());
        return ResponseEntity.ok(ApiResponse.exito("Sesión cerrada exitosamente para el usuario " + request.getUsername() + " (loggeado = false)", null));
    }
}
