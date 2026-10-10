package com.kapm.onboarding_clientes.service;

import com.kapm.onboarding_clientes.dto.request.ActualizarContactoRequest;
import com.kapm.onboarding_clientes.dto.request.ClienteRegistrationRequest;
import com.kapm.onboarding_clientes.dto.request.ClienteUpdateRequest;
import com.kapm.onboarding_clientes.dto.request.LoginRequest;
import com.kapm.onboarding_clientes.dto.response.ClienteResponse;
import com.kapm.onboarding_clientes.dto.response.CuentaResponse;
import com.kapm.onboarding_clientes.dto.response.LoginResponse;
import com.kapm.onboarding_clientes.dto.response.ValidacionIdentificadorResponse;

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

    // Actualización identificando por CURP y por RFC
    ClienteResponse actualizarClientePorCurp(String curp, ClienteUpdateRequest request);

    ClienteResponse actualizarClientePorRfc(String rfc, ClienteUpdateRequest request);

    // Baja Lógica identificando por CURP y por RFC
    void desactivarCliente(Long id);

    void desactivarClientePorCurp(String curp);

    void desactivarClientePorRfc(String rfc);

    // Reactivación identificando por CURP y por RFC
    ClienteResponse reactivarClientePorCurp(String curp);

    ClienteResponse reactivarClientePorRfc(String rfc);

    // Validación y disponibilidad en tiempo real
    ValidacionIdentificadorResponse validarDisponibilidadCurp(String curp);

    ValidacionIdentificadorResponse validarDisponibilidadRfc(String rfc);

    // Actualización rápida de datos de contacto por CURP y RFC
    ClienteResponse actualizarContactoPorCurp(String curp, ActualizarContactoRequest request);

    ClienteResponse actualizarContactoPorRfc(String rfc, ActualizarContactoRequest request);

    // Consulta directa de Cuenta y Saldo por CURP y RFC
    CuentaResponse obtenerCuentaPorCurp(String curp);

    CuentaResponse obtenerCuentaPorRfc(String rfc);

    LoginResponse autenticarCliente(LoginRequest request);
}
