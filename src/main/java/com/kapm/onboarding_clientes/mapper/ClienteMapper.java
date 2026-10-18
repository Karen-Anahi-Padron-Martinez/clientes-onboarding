package com.kapm.onboarding_clientes.mapper;

import com.kapm.onboarding_clientes.dto.request.DatosLaboralesDTO;
import com.kapm.onboarding_clientes.dto.request.DomicilioDTO;
import com.kapm.onboarding_clientes.dto.response.ClienteResponse;
import com.kapm.onboarding_clientes.dto.response.CuentaResponse;
import com.kapm.onboarding_clientes.dto.response.SaldoResponse;
import com.kapm.onboarding_clientes.entity.*;
import com.kapm.onboarding_clientes.repository.DatosSeguridadBiometriaRepository;
import com.kapm.onboarding_clientes.service.SeguridadBiometriaServiceImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.Optional;

/**
 * Componente Mapper dedicado para la transformación desacoplada entre entidades de dominio y DTOs.
 * Cumple con el principio de responsabilidad única (SRP), eliminando la lógica de mapeo de los servicios.
 */
@Component
public class ClienteMapper {

    private final DatosSeguridadBiometriaRepository seguridadRepository;
    private final long inactividadLimiteSegundos;

    public ClienteMapper(
            DatosSeguridadBiometriaRepository seguridadRepository,
            @Value("${app.seguridad.inactividad-segundos:5}") long inactividadLimiteSegundos) {
        this.seguridadRepository = seguridadRepository;
        this.inactividadLimiteSegundos = inactividadLimiteSegundos > 0 
                ? inactividadLimiteSegundos 
                : SeguridadBiometriaServiceImpl.INACTIVITY_TIMEOUT_SECONDS;
    }

    public ClienteResponse mapearAClienteResponse(Cliente cliente) {
        if (cliente == null) {
            return null;
        }

        int edad = (cliente.getFechaNacimiento() != null)
                ? Period.between(cliente.getFechaNacimiento(), LocalDate.now()).getYears()
                : 0;

        String nombreCompleto = construirNombreCompleto(cliente);
        DomicilioDTO domDTO = mapearADomicilioDTO(cliente.getDomicilio());
        DatosLaboralesDTO labDTO = mapearADatosLaboralesDTO(cliente);
        CuentaResponse cuentaResponse = mapearACuentaResponseConCliente(cliente.getCuenta(), cliente);

        String username = null;
        TipoBiometria tipoBio = null;
        Boolean biometriaRegistrada = false;
        Boolean loggeado = false;
        Long segundosInactividad = null;

        if (cliente.getDatosSeguridad() != null) {
            DatosSeguridadBiometria seguridad = cliente.getDatosSeguridad();
            username = seguridad.getUsername();
            tipoBio = seguridad.getTipoBiometria();
            biometriaRegistrada = (seguridad.getHashBiometrico() != null && !seguridad.getHashBiometrico().isBlank());

            if (seguridad.getUltimaActividad() != null) {
                segundosInactividad = Duration.between(seguridad.getUltimaActividad(), LocalDateTime.now()).getSeconds();

                if (Boolean.TRUE.equals(seguridad.getLoggeado()) && segundosInactividad <= inactividadLimiteSegundos) {
                    loggeado = true;
                    seguridad.setUltimaActividad(LocalDateTime.now());
                } else {
                    seguridad.setLoggeado(false);
                    loggeado = false;
                }
                seguridadRepository.save(seguridad);
            } else {
                loggeado = Boolean.TRUE.equals(seguridad.getLoggeado());
            }
        }

        return ClienteResponse.builder()
                .id(null)
                .nombre(cliente.getNombre())
                .segundoNombre(cliente.getSegundoNombre())
                .apellidoPaterno(cliente.getApellidoPaterno())
                .apellidoMaterno(cliente.getApellidoMaterno())
                .nombreCompleto(nombreCompleto)
                .fechaNacimiento(cliente.getFechaNacimiento())
                .edad(edad)
                .curp(cliente.getCurp())
                .rfc(cliente.getRfc())
                .sexo(cliente.getSexo() != null ? cliente.getSexo().getCodigo() : null)
                .nacionalidad(cliente.getNacionalidad() != null ? cliente.getNacionalidad().getDescripcion() : null)
                .estadoCivil(cliente.getEstadoCivil() != null ? cliente.getEstadoCivil().getCodigo() : null)
                .correo(cliente.getCorreo())
                .telefonoMovil(cliente.getTelefonoMovil())
                .telefonoAlternativo(cliente.getTelefonoAlternativo())
                .domicilio(domDTO)
                .laboral(labDTO)
                .cuenta(cuentaResponse)
                .username(username)
                .loggeado(loggeado)
                .tipoBiometriaRegistrada(tipoBio)
                .biometriaRegistrada(biometriaRegistrada)
                .segundosUltimaActividad(segundosInactividad)
                .activo(cliente.getActivo())
                .fechaRegistro(cliente.getFechaRegistro())
                .build();
    }

    public DomicilioDTO mapearADomicilioDTO(Domicilio dom) {
        if (dom == null) {
            return null;
        }
        return DomicilioDTO.builder()
                .calle(dom.getCalle())
                .numeroExterior(dom.getNumeroExterior())
                .numeroInterior(dom.getNumeroInterior())
                .colonia(dom.getColonia())
                .municipio(dom.getMunicipio())
                .estado(dom.getEstado() != null ? dom.getEstado().getDescripcion() : null)
                .codigoPostal(dom.getCodigoPostal())
                .pais(dom.getPais() != null ? dom.getPais().getDescripcion() : null)
                .build();
    }

    public DatosLaboralesDTO mapearADatosLaboralesDTO(Cliente cliente) {
        if (cliente == null) {
            return null;
        }
        return DatosLaboralesDTO.builder()
                .ocupacion(cliente.getOcupacion() != null ? cliente.getOcupacion().getDescripcion() : null)
                .empresa(cliente.getEmpresa())
                .ingresoMensual(cliente.getIngresoMensual() != null
                        ? cliente.getIngresoMensual().setScale(2, RoundingMode.HALF_UP)
                        : null)
                .build();
    }

    public CuentaResponse mapearACuentaResponseConCliente(Cuenta cuenta, Cliente cliente) {
        if (cuenta == null) {
            return null;
        }
        String nombreCompleto = cliente != null ? construirNombreCompleto(cliente) : "No asignado";
        String curp = cliente != null ? cliente.getCurp() : "N/A";

        return CuentaResponse.builder()
                .id(null)
                .numeroCuenta(cuenta.getNumeroCuenta())
                .saldo(cuenta.getSaldo() != null ? cuenta.getSaldo().setScale(2, RoundingMode.HALF_UP) : null)
                .estatus(cuenta.getEstatus())
                .fechaCreacion(cuenta.getFechaCreacion())
                .clienteNombreCompleto(nombreCompleto)
                .clienteCurp(curp)
                .build();
    }

    public CuentaResponse mapearACuentaResponse(Cuenta cuenta, Optional<Cliente> clienteOpt) {
        if (cuenta == null) {
            return null;
        }
        String titularNombre = clienteOpt.map(this::construirNombreCompleto).orElse("No asignado");
        String titularCurp = clienteOpt.map(Cliente::getCurp).orElse("N/A");

        return CuentaResponse.builder()
                .id(null)
                .numeroCuenta(cuenta.getNumeroCuenta())
                .saldo(cuenta.getSaldo() != null ? cuenta.getSaldo().setScale(2, RoundingMode.HALF_UP) : null)
                .estatus(cuenta.getEstatus())
                .fechaCreacion(cuenta.getFechaCreacion())
                .clienteNombreCompleto(titularNombre)
                .clienteCurp(titularCurp)
                .build();
    }

    public SaldoResponse mapearASaldoResponse(Cuenta cuenta, Optional<Cliente> clienteOpt, Saldo saldoDetalle) {
        String titularNombre = clienteOpt.map(this::construirNombreCompleto).orElse("No asignado");
        String titularCurp = clienteOpt.map(Cliente::getCurp).orElse("N/A");

        boolean loggeado = false;
        long segundosInactividad = 0;

        if (clienteOpt.isPresent() && clienteOpt.get().getDatosSeguridad() != null) {
            DatosSeguridadBiometria seguridad = clienteOpt.get().getDatosSeguridad();
            if (seguridad.getUltimaActividad() != null) {
                segundosInactividad = Duration.between(seguridad.getUltimaActividad(), LocalDateTime.now()).getSeconds();
                if (Boolean.TRUE.equals(seguridad.getLoggeado()) && segundosInactividad <= inactividadLimiteSegundos) {
                    loggeado = true;
                    seguridad.setUltimaActividad(LocalDateTime.now());
                } else {
                    seguridad.setLoggeado(false);
                    loggeado = false;
                }
                seguridadRepository.save(seguridad);
            } else {
                loggeado = Boolean.TRUE.equals(seguridad.getLoggeado());
            }
        }

        return SaldoResponse.builder()
                .numeroCuenta(cuenta.getNumeroCuenta())
                .saldoDisponible(saldoDetalle.getSaldoDisponible() != null
                        ? saldoDetalle.getSaldoDisponible().setScale(2, RoundingMode.HALF_UP)
                        : null)
                .saldoContable(saldoDetalle.getSaldoContable() != null
                        ? saldoDetalle.getSaldoContable().setScale(2, RoundingMode.HALF_UP)
                        : null)
                .estatus(cuenta.getEstatus())
                .titularNombreCompleto(titularNombre)
                .titularCurp(titularCurp)
                .usuarioLoggeado(loggeado)
                .segundosInactividad(segundosInactividad)
                .fechaConsulta(LocalDateTime.now())
                .build();
    }

    private String construirNombreCompleto(Cliente cliente) {
        if (cliente == null) {
            return "";
        }
        return cliente.getNombre() +
                (cliente.getSegundoNombre() != null && !cliente.getSegundoNombre().isBlank() ? " " + cliente.getSegundoNombre() : "") +
                " " + cliente.getApellidoPaterno() + " " + cliente.getApellidoMaterno();
    }
}
