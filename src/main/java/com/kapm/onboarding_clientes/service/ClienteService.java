package com.kapm.onboarding_clientes.service;

import com.kapm.onboarding_clientes.dto.request.*;
import com.kapm.onboarding_clientes.dto.response.ClienteResponse;
import com.kapm.onboarding_clientes.dto.response.CuentaResponse;
import com.kapm.onboarding_clientes.dto.response.LoginResponse;
import com.kapm.onboarding_clientes.dto.response.PageResponse;
import com.kapm.onboarding_clientes.dto.response.ValidacionIdentificadorResponse;

import java.util.List;

public interface ClienteService {

    ClienteResponse registrarCliente(ClienteRegistrationRequest request);

    List<ClienteResponse> obtenerTodosLosClientes();

    List<ClienteResponse> obtenerClientesActivos();

    // Búsqueda dinámica con filtros combinables y paginación (PageResponse)
    PageResponse<ClienteResponse> buscarClientes(BuscarClienteRequest request);

    // Búsqueda por identificadores oficiales individuales
    ClienteResponse obtenerClientePorCurp(String curp);

    ClienteResponse obtenerClientePorRfc(String rfc);

    ClienteResponse obtenerClientePorCorreo(String correo);

    ClienteResponse obtenerClientePorNumeroCuenta(String numeroCuenta);

    // Actualización de cliente
    ClienteResponse actualizarCliente(ClienteUpdateRequest request);

    ClienteResponse actualizarClientePorCurp(String curp, ClienteUpdateRequest request);

    ClienteResponse actualizarClientePorCurp(ActualizarPorCurpRequest request);

    ClienteResponse actualizarClientePorRfc(String rfc, ClienteUpdateRequest request);

    ClienteResponse actualizarClientePorRfc(ActualizarPorRfcRequest request);

    // Baja Lógica identificando exclusivamente por CURP o RFC
    void desactivarCliente(ClienteIdentificadorRequest request);

    void desactivarClientePorCurp(String curp);

    void desactivarClientePorRfc(String rfc);

    // Reactivación identificando por CURP o RFC
    ClienteResponse reactivarCliente(ClienteIdentificadorRequest request);

    ClienteResponse reactivarClientePorCurp(String curp);

    ClienteResponse reactivarClientePorRfc(String rfc);

    // Validación y disponibilidad en tiempo real
    ValidacionIdentificadorResponse validarDisponibilidad(ValidarIdentificadorRequest request);

    ValidacionIdentificadorResponse validarDisponibilidadCurp(String curp);

    ValidacionIdentificadorResponse validarDisponibilidadRfc(String rfc);

    // Actualización rápida de datos de contacto
    ClienteResponse actualizarContacto(ActualizarContactoRequest request);

    ClienteResponse actualizarContactoPorCurp(String curp, ActualizarContactoRequest request);

    ClienteResponse actualizarContactoPorRfc(String rfc, ActualizarContactoRequest request);

    // Consulta directa de Cuenta y Saldo
    CuentaResponse obtenerCuenta(ClienteIdentificadorRequest request);

    CuentaResponse obtenerCuentaPorCurp(String curp);

    CuentaResponse obtenerCuentaPorRfc(String rfc);

    LoginResponse autenticarCliente(LoginRequest request);
}
