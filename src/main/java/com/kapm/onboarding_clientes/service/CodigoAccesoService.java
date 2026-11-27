package com.kapm.onboarding_clientes.service;

import com.kapm.onboarding_clientes.dto.response.CodigoVerificacionResponse;
import com.kapm.onboarding_clientes.entity.Cliente;
import com.kapm.onboarding_clientes.entity.PropositoCodigoAcceso;

/**
 * Códigos de 6 dígitos enviados al correo registrado para el segundo factor del login y la recuperación de contraseña.
 */
public interface CodigoAccesoService {

    /** Invalida los códigos anteriores del mismo propósito, genera uno nuevo y lo envía al correo del cliente. */
    CodigoVerificacionResponse emitir(String username, Cliente cliente, PropositoCodigoAcceso proposito);

    /** Valida y consume el código más reciente; lanza ReglaNegocioException si es incorrecto, expiró o ya se usó. */
    void validar(String username, PropositoCodigoAcceso proposito, String codigo);
}
