package com.kapm.onboarding_clientes.service;

import com.kapm.onboarding_clientes.dto.response.LoginResponse;
import com.kapm.onboarding_clientes.entity.DatosSeguridadBiometria;
import com.kapm.onboarding_clientes.entity.TipoBiometria;
import com.kapm.onboarding_clientes.exception.ErrorMessage;
import com.kapm.onboarding_clientes.exception.RecursoNoEncontradoException;
import com.kapm.onboarding_clientes.exception.ReglaNegocioException;
import com.kapm.onboarding_clientes.repository.DatosSeguridadBiometriaRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HexFormat;

@Slf4j
@Service
public class SeguridadBiometriaServiceImpl implements SeguridadBiometriaService {

    public static final long INACTIVITY_TIMEOUT_SECONDS = 5L;

    @Value("${app.seguridad.inactividad-segundos:5}")
    private long inactividadLimiteSegundos;

    private final PasswordEncoder passwordEncoder;
    private final DatosSeguridadBiometriaRepository seguridadRepository;

    public SeguridadBiometriaServiceImpl(DatosSeguridadBiometriaRepository seguridadRepository) {
        this.passwordEncoder = new BCryptPasswordEncoder();
        this.seguridadRepository = seguridadRepository;
        this.inactividadLimiteSegundos = INACTIVITY_TIMEOUT_SECONDS;
    }

    @Override
    public String encriptarPassword(String plainPassword) {
        if (plainPassword == null || plainPassword.isBlank()) {
            throw new ReglaNegocioException(ErrorMessage.AUTH_PASSWORD_VACIA);
        }
        return passwordEncoder.encode(plainPassword);
    }

    @Override
    public boolean validarPassword(String plainPassword, String hashedPassword) {
        if (plainPassword == null || hashedPassword == null) {
            return false;
        }
        return passwordEncoder.matches(plainPassword, hashedPassword);
    }

    @Override
    public String generarHashBiometrico(String plantillaBiometrica) {
        if (plantillaBiometrica == null || plantillaBiometrica.isBlank()) {
            throw new ReglaNegocioException(ErrorMessage.AUTH_BIOMETRIA_VACIA);
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(plantillaBiometrica.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            log.error("Fallo algorítmico al generar hash SHA-256 para biometría", e);
            throw new ReglaNegocioException(ErrorMessage.AUTH_HASH_ERROR);
        }
    }

    @Override
    public boolean validarBiometria(String plantillaCapturada, String hashAlmacenado) {
        if (plantillaCapturada == null || hashAlmacenado == null) {
            return false;
        }
        String hashCalculado = generarHashBiometrico(plantillaCapturada);
        return hashCalculado.equalsIgnoreCase(hashAlmacenado);
    }

    @Override
    public DatosSeguridadBiometria crearDatosSeguridad(String username, String rawPassword, TipoBiometria tipoBiometria, String rawBiometria) {
        if (seguridadRepository.existsByUsername(username)) {
            throw new ReglaNegocioException(ErrorMessage.AUTH_USUARIO_DUPLICADO, username);
        }

        String passwordHash = encriptarPassword(rawPassword);
        String hashBiometrico = generarHashBiometrico(rawBiometria);

        return DatosSeguridadBiometria.builder()
                .username(username)
                .passwordHash(passwordHash)
                .tipoBiometria(tipoBiometria != null ? tipoBiometria : TipoBiometria.HUELLA_DACTILAR)
                .hashBiometrico(hashBiometrico)
                .build();
    }

    @Override
    public LoginResponse consultarEstadoSesion(String username) {
        DatosSeguridadBiometria seguridad = seguridadRepository.findByUsername(username)
                .orElseThrow(() -> new RecursoNoEncontradoException(ErrorMessage.AUTH_USUARIO_NO_ENCONTRADO, username));

        boolean loggeado = false;
        long segundosInactividad = 0;
        String estado;

        if (seguridad.getUltimaActividad() != null) {
            segundosInactividad = Duration.between(seguridad.getUltimaActividad(), LocalDateTime.now()).getSeconds();
            if (Boolean.TRUE.equals(seguridad.getLoggeado()) && segundosInactividad <= inactividadLimiteSegundos) {
                loggeado = true;
                estado = "SESIÓN ACTIVA (Última actividad hace " + segundosInactividad + "s)";
                seguridad.setUltimaActividad(LocalDateTime.now());
            } else {
                seguridad.setLoggeado(false);
                loggeado = false;
                estado = "SESIÓN EXPIRADA POR INACTIVIDAD (Superó el límite de " + inactividadLimiteSegundos + "s: " + segundosInactividad + "s transcurridos)";
            }
            seguridadRepository.save(seguridad);
        } else {
            loggeado = Boolean.TRUE.equals(seguridad.getLoggeado());
            estado = loggeado ? "SESIÓN ACTIVA" : "SESIÓN INACTIVA";
        }

        return LoginResponse.builder()
                .autenticado(loggeado)
                .loggeado(loggeado)
                .username(seguridad.getUsername())
                .tipoBiometriaValidada(seguridad.getTipoBiometria())
                .segundosDesdeUltimaActividad(segundosInactividad)
                .estadoSesion(estado)
                .mensaje("Estado de sesión de usuario: loggeado=" + loggeado)
                .timestamp(LocalDateTime.now())
                .build();
    }

    @Override
    public void cerrarSesion(String username) {
        DatosSeguridadBiometria seguridad = seguridadRepository.findByUsername(username)
                .orElseThrow(() -> new RecursoNoEncontradoException(ErrorMessage.AUTH_USUARIO_NO_ENCONTRADO, username));

        seguridad.setLoggeado(false);
        seguridad.setSesionId(null);
        seguridadRepository.save(seguridad);
    }
}
