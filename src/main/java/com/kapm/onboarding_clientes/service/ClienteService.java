package com.kapm.onboarding_clientes.service;

import com.kapm.onboarding_clientes.dto.request.ClienteRegistrationRequest;
import com.kapm.onboarding_clientes.dto.request.ClienteUpdateRequest;
import com.kapm.onboarding_clientes.dto.request.LoginRequest;
import com.kapm.onboarding_clientes.dto.response.ClienteResponse;
import com.kapm.onboarding_clientes.dto.response.LoginResponse;

import java.time.LocalDate;
import java.util.List;

public interface ClienteService {

    ClienteResponse registrarCliente(ClienteRegistrationRequest request);

    List<ClienteResponse> obtenerTodosLosClientes();

    List<ClienteResponse> obtenerClientesActivos();

    ClienteResponse obtenerClientePorId(Long id);

    ClienteResponse obtenerClientePorCurp(String curp);

    ClienteResponse obtenerClientePorRfc(String rfc);

    ClienteResponse obtenerClientePorCorreo(String correo);

    ClienteResponse obtenerClientePorNumeroCuenta(String numeroCuenta);

    List<ClienteResponse> obtenerClientesPorRangoFechas(LocalDate fechaInicio, LocalDate fechaFin);

    ClienteResponse actualizarCliente(Long id, ClienteUpdateRequest request);

    void desactivarCliente(Long id);

    LoginResponse autenticarCliente(LoginRequest request);
}
