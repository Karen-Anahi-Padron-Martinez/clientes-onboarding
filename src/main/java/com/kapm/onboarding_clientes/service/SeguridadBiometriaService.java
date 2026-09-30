package com.kapm.onboarding_clientes.service;

import com.kapm.onboarding_clientes.model.DatosSeguridadBiometria;
import com.kapm.onboarding_clientes.model.TipoBiometria;
import com.kapm.onboarding_clientes.repository.DatosSeguridadBiometriaRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Service
public class SeguridadBiometriaService {

    private final PasswordEncoder passwordEncoder;
    private final DatosSeguridadBiometriaRepository seguridadRepository;

    public SeguridadBiometriaService(DatosSeguridadBiometriaRepository seguridadRepository) {
        this.passwordEncoder = new BCryptPasswordEncoder();
        this.seguridadRepository = seguridadRepository;
    }

    /**
     * Encripta una contraseña utilizando el algoritmo BCrypt con Salt.
     */
    public String encriptarPassword(String plainPassword) {
        if (plainPassword == null || plainPassword.isBlank()) {
            throw new IllegalArgumentException("La contraseña no puede estar vacía.");
        }
        return passwordEncoder.encode(plainPassword);
    }

    /**
     * Valida una contraseña contra su hash BCrypt.
     */
    public boolean validarPassword(String plainPassword, String hashedPassword) {
        return passwordEncoder.matches(plainPassword, hashedPassword);
    }

    /**
     * Genera un hash criptográfico SHA-256 para plantillas o patrones biométricos.
     */
    public String generarHashBiometrico(String plantillaBiometrica) {
        if (plantillaBiometrica == null || plantillaBiometrica.isBlank()) {
            throw new IllegalArgumentException("La plantilla biométrica no puede estar vacía.");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(plantillaBiometrica.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error al generar hash criptográfico para biometría", e);
        }
    }

    /**
     * Valida una plantilla biométrica capturada contra el hash almacenado.
     */
    public boolean validarBiometria(String plantillaCapturada, String hashAlmacenado) {
        if (plantillaCapturada == null || hashAlmacenado == null) {
            return false;
        }
        String hashCalculado = generarHashBiometrico(plantillaCapturada);
        return hashCalculado.equalsIgnoreCase(hashAlmacenado);
    }

    /**
     * Crea una entidad de seguridad biométrica con credenciales cifradas.
     */
    public DatosSeguridadBiometria crearDatosSeguridad(String username, String rawPassword, TipoBiometria tipoBiometria, String rawBiometria) {
        if (seguridadRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("El nombre de usuario '" + username + "' ya está registrado");
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
}
