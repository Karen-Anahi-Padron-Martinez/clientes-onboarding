package com.kapm.onboarding_clientes.service;

import com.kapm.onboarding_clientes.dto.request.LoginRequest;
import com.kapm.onboarding_clientes.dto.request.RestablecerPasswordRequest;
import com.kapm.onboarding_clientes.dto.request.SolicitarRecuperacionRequest;
import com.kapm.onboarding_clientes.dto.request.VerificarLoginRequest;
import com.kapm.onboarding_clientes.dto.response.LoginDesafioResponse;
import com.kapm.onboarding_clientes.dto.response.LoginResponse;

/**
 * Login en dos pasos (contraseña + código al correo) y recuperación de contraseña por correo.
 */
public interface AccesoClienteService {

    /** Paso 1: valida usuario y contraseña y envía el código al correo. No emite token. */
    LoginDesafioResponse iniciarLogin(LoginRequest request);

    /** Paso 2: valida el código y emite el JWT de sesión. */
    LoginResponse verificarLogin(VerificarLoginRequest request);

    /** Envía un código de recuperación. Responde igual exista o no el usuario, para no revelar cuentas. */
    void solicitarRecuperacion(SolicitarRecuperacionRequest request);

    /** Valida el código de recuperación, guarda la nueva contraseña y cierra cualquier sesión abierta. */
    void restablecerPassword(RestablecerPasswordRequest request);
}
