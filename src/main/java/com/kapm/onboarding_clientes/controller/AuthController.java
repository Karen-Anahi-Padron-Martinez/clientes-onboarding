package com.kapm.onboarding_clientes.controller;

import com.kapm.onboarding_clientes.dto.request.LoginRequest;
import com.kapm.onboarding_clientes.dto.response.ApiResponse;
import com.kapm.onboarding_clientes.dto.response.LoginResponse;
import com.kapm.onboarding_clientes.exception.RecursoNoEncontradoException;
import com.kapm.onboarding_clientes.model.DatosSeguridadBiometria;
import com.kapm.onboarding_clientes.repository.DatosSeguridadBiometriaRepository;
import com.kapm.onboarding_clientes.service.ClienteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticación y Biometría", description = "Endpoints para la validación de credenciales cifradas, firmas biométricas y control de sesión con temporizador de inactividad (5s)")
public class AuthController {

    private final ClienteService clienteService;
    private final DatosSeguridadBiometriaRepository seguridadRepository;

    @PostMapping("/login")
    @Operation(summary = "Autenticación segura (Password Encriptada y/o Biometría)", description = "Permite autenticar a un usuario registrado mediante su contraseña cifrada con BCrypt o su patrón biométrico encriptado en SHA-256. Activa la bandera 'loggeado = true' e inicia el contador de actividad (Límite: 5s).")
    public ResponseEntity<ApiResponse<LoginResponse>> autenticar(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = clienteService.autenticarCliente(request);
        if (response.isAutenticado()) {
            return ResponseEntity.ok(ApiResponse.exito(response.getMensaje(), response));
        } else {
            return ResponseEntity.badRequest().body(ApiResponse.error(response.getMensaje()));
        }
    }

    @GetMapping("/estado/{username}")
    @Operation(summary = "Consultar estado de sesión y temporizador de inactividad (5s)", description = "Retorna el valor 'loggeado' (true/false) del usuario especificando el tiempo transcurrido desde su última actividad. Si supera los 5 segundos de inactividad, la sesión pasa automáticamente a false.")
    public ResponseEntity<ApiResponse<LoginResponse>> consultarEstadoSesion(@PathVariable String username) {
        DatosSeguridadBiometria seguridad = seguridadRepository.findByUsername(username)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado: " + username));

        boolean loggeado = false;
        long segundosInactividad = 0;
        String estado;

        if (seguridad.getUltimaActividad() != null) {
            segundosInactividad = Duration.between(seguridad.getUltimaActividad(), LocalDateTime.now()).getSeconds();
            if (Boolean.TRUE.equals(seguridad.getLoggeado()) && segundosInactividad <= 5) {
                loggeado = true;
                estado = "SESIÓN ACTIVA (Última actividad hace " + segundosInactividad + "s)";
                seguridad.setUltimaActividad(LocalDateTime.now());
            } else {
                seguridad.setLoggeado(false);
                loggeado = false;
                estado = "SESIÓN EXPIRADA POR INACTIVIDAD (Superó el límite de 5s: " + segundosInactividad + "s transcurridos)";
            }
            seguridadRepository.save(seguridad);
        } else {
            loggeado = Boolean.TRUE.equals(seguridad.getLoggeado());
            estado = loggeado ? "SESIÓN ACTIVA" : "SESIÓN INACTIVA";
        }

        LoginResponse response = LoginResponse.builder()
                .autenticado(loggeado)
                .loggeado(loggeado)
                .username(seguridad.getUsername())
                .tipoBiometriaValidada(seguridad.getTipoBiometria())
                .segundosDesdeUltimaActividad(segundosInactividad)
                .estadoSesion(estado)
                .mensaje("Estado de sesión de usuario: loggeado=" + loggeado)
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.ok(ApiResponse.exito(response.getMensaje(), response));
    }

    @PostMapping("/logout/{username}")
    @Operation(summary = "Cerrar Sesión de Usuario", description = "Cambia manualmente el estado 'loggeado' a false para el usuario especificado.")
    public ResponseEntity<ApiResponse<Void>> logout(@PathVariable String username) {
        DatosSeguridadBiometria seguridad = seguridadRepository.findByUsername(username)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado: " + username));

        seguridad.setLoggeado(false);
        seguridadRepository.save(seguridad);

        return ResponseEntity.ok(ApiResponse.exito("Sesión cerrada exitosamente para el usuario " + username + " (loggeado = false)", null));
    }
}
