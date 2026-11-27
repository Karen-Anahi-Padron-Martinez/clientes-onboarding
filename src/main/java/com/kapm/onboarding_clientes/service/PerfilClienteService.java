package com.kapm.onboarding_clientes.service;

import com.kapm.onboarding_clientes.dto.request.ActualizarPerfilRequest;
import com.kapm.onboarding_clientes.dto.request.CambiarPasswordRequest;
import com.kapm.onboarding_clientes.dto.response.PerfilResponse;
import com.kapm.onboarding_clientes.dto.response.SaldoResponse;

/** Consultas y ajustes del cliente autenticado; el titular siempre sale del subject del JWT. */
public interface PerfilClienteService {

    PerfilResponse obtenerPerfil(String username);

    /** Actualiza los datos del titular; CURP y RFC no se modifican. */
    PerfilResponse actualizarPerfil(String username, ActualizarPerfilRequest request);

    SaldoResponse obtenerSaldo(String username);

    void cambiarPassword(String username, CambiarPasswordRequest request);
}
