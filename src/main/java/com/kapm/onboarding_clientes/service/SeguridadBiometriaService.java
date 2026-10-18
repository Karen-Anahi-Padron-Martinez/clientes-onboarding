package com.kapm.onboarding_clientes.service;

import com.kapm.onboarding_clientes.dto.response.LoginResponse;
import com.kapm.onboarding_clientes.entity.DatosSeguridadBiometria;
import com.kapm.onboarding_clientes.entity.TipoBiometria;

/**
 * Interfaz del servicio de seguridad y biometría.
 * Gestiona el cifrado con BCrypt, hashes SHA-256 de patrones biométricos y control de sesión con límite de inactividad.
 */
public interface SeguridadBiometriaService {

    String encriptarPassword(String plainPassword);

    boolean validarPassword(String plainPassword, String hashedPassword);

    String generarHashBiometrico(String plantillaBiometrica);

    boolean validarBiometria(String plantillaCapturada, String hashAlmacenado);

    DatosSeguridadBiometria crearDatosSeguridad(String username, String rawPassword, TipoBiometria tipoBiometria, String rawBiometria);

    LoginResponse consultarEstadoSesion(String username);

    void cerrarSesion(String username);
}
