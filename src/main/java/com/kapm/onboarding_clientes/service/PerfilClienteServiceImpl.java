package com.kapm.onboarding_clientes.service;

import com.kapm.onboarding_clientes.dto.request.ActualizarPerfilRequest;
import com.kapm.onboarding_clientes.dto.request.CambiarPasswordRequest;
import com.kapm.onboarding_clientes.dto.request.DomicilioDTO;
import com.kapm.onboarding_clientes.dto.response.PerfilResponse;
import com.kapm.onboarding_clientes.dto.response.SaldoResponse;
import com.kapm.onboarding_clientes.entity.Cliente;
import com.kapm.onboarding_clientes.entity.DatosSeguridadBiometria;
import com.kapm.onboarding_clientes.entity.Domicilio;
import com.kapm.onboarding_clientes.exception.AccesoDenegadoException;
import com.kapm.onboarding_clientes.exception.CorreoDuplicadoException;
import com.kapm.onboarding_clientes.exception.ErrorMessage;
import com.kapm.onboarding_clientes.exception.RecursoNoEncontradoException;
import com.kapm.onboarding_clientes.exception.ReglaNegocioException;
import com.kapm.onboarding_clientes.mapper.ClienteMapper;
import com.kapm.onboarding_clientes.repository.ClienteRepository;
import com.kapm.onboarding_clientes.repository.DatosSeguridadBiometriaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Period;

@Service
@RequiredArgsConstructor
public class PerfilClienteServiceImpl implements PerfilClienteService {

    private final ClienteRepository clienteRepository;
    private final DatosSeguridadBiometriaRepository seguridadRepository;
    private final CuentaService cuentaService;
    private final SeguridadBiometriaService seguridadBiometriaService;
    private final CatalogoService catalogoService;
    private final ClienteMapper clienteMapper;

    @Override
    @Transactional(readOnly = true)
    public PerfilResponse obtenerPerfil(String username) {
        return aPerfil(username, clientePorUsername(username));
    }

    @Override
    @Transactional
    public PerfilResponse actualizarPerfil(String username, ActualizarPerfilRequest request) {
        Cliente cliente = clientePorUsername(username);

        validarFechaNacimiento(request.getFechaNacimiento());

        String correo = request.getCorreo().trim().toLowerCase();
        if (!correo.equals(cliente.getCorreo())) {
            // El correo recibe los códigos de acceso: cambiarlo exige demostrar que se conoce la contraseña
            DatosSeguridadBiometria seguridad = cliente.getDatosSeguridad();
            if (request.getPasswordActual() == null || request.getPasswordActual().isBlank()) {
                throw new ReglaNegocioException(ErrorMessage.AUTH_PASSWORD_REQUERIDA_CAMBIO_CORREO);
            }
            if (seguridad == null || !seguridadBiometriaService.validarPassword(request.getPasswordActual(), seguridad.getPasswordHash())) {
                throw new ReglaNegocioException(ErrorMessage.AUTH_PASSWORD_ACTUAL_INCORRECTA);
            }
            if (clienteRepository.existsByCorreoAndIdNot(correo, cliente.getId())) {
                throw new CorreoDuplicadoException(ErrorMessage.CORREO_DUPLICADO);
            }
            cliente.setCorreo(correo);
        }

        cliente.setNombre(request.getNombre().trim());
        cliente.setSegundoNombre(textoOpcional(request.getSegundoNombre()));
        cliente.setApellidoPaterno(request.getApellidoPaterno().trim());
        cliente.setApellidoMaterno(request.getApellidoMaterno().trim());
        cliente.setFechaNacimiento(request.getFechaNacimiento());
        cliente.setSexo(catalogoService.buscarSexo(request.getSexo()));
        cliente.setNacionalidad(catalogoService.buscarNacionalidad(request.getNacionalidad()));
        cliente.setEstadoCivil(catalogoService.buscarEstadoCivil(request.getEstadoCivil()));
        cliente.setTelefonoMovil(request.getTelefonoMovil().trim());
        cliente.setTelefonoAlternativo(textoOpcional(request.getTelefonoAlternativo()));
        cliente.setOcupacion(catalogoService.buscarOcupacion(request.getOcupacion()));
        cliente.setEmpresa(textoOpcional(request.getEmpresa()));
        cliente.setIngresoMensual(request.getIngresoMensual());

        Domicilio dom = cliente.getDomicilio() != null ? cliente.getDomicilio() : new Domicilio();
        DomicilioDTO nuevo = request.getDomicilio();
        dom.setCalle(nuevo.getCalle().trim());
        dom.setNumeroExterior(nuevo.getNumeroExterior().trim());
        dom.setNumeroInterior(textoOpcional(nuevo.getNumeroInterior()));
        dom.setColonia(nuevo.getColonia().trim());
        dom.setMunicipio(nuevo.getMunicipio().trim());
        dom.setEstado(catalogoService.buscarEstadoRepublica(nuevo.getEstado()));
        dom.setCodigoPostal(nuevo.getCodigoPostal().trim());
        dom.setPais(catalogoService.buscarPais(nuevo.getPais()));
        cliente.setDomicilio(dom);

        // CURP y RFC no se tocan: además de no estar en el request, la entidad los marca updatable = false
        return aPerfil(username, clienteRepository.save(cliente));
    }

    @Override
    @Transactional
    public SaldoResponse obtenerSaldo(String username) {
        Cliente cliente = clientePorUsername(username);
        if (cliente.getCuenta() == null) {
            throw new RecursoNoEncontradoException(ErrorMessage.CLIENTE_SIN_CUENTA);
        }
        return cuentaService.obtenerSaldoCuenta(cliente.getCuenta().getNumeroCuenta());
    }

    @Override
    @Transactional
    public void cambiarPassword(String username, CambiarPasswordRequest request) {
        DatosSeguridadBiometria seguridad = seguridadRepository.findByUsername(username)
                .orElseThrow(AccesoDenegadoException::new);

        if (!seguridadBiometriaService.validarPassword(request.getPasswordActual(), seguridad.getPasswordHash())) {
            throw new ReglaNegocioException(ErrorMessage.AUTH_PASSWORD_ACTUAL_INCORRECTA);
        }
        if (seguridadBiometriaService.validarPassword(request.getNuevaPassword(), seguridad.getPasswordHash())) {
            throw new ReglaNegocioException(ErrorMessage.AUTH_PASSWORD_REPETIDA);
        }
        // La sesión actual sigue vigente; solo cambia la credencial para los próximos logins
        seguridad.setPasswordHash(seguridadBiometriaService.encriptarPassword(request.getNuevaPassword()));
        seguridadRepository.save(seguridad);
    }

    private Cliente clientePorUsername(String username) {
        return clienteRepository.findByDatosSeguridadUsername(username)
                .orElseThrow(AccesoDenegadoException::new);
    }

    /** Mayor de edad (la CURP y el RFC solo se validan por formato). */
    private static void validarFechaNacimiento(LocalDate fecha) {
        if (fecha.isAfter(LocalDate.now())) {
            throw new ReglaNegocioException(ErrorMessage.FECHA_NACIMIENTO_FUTURA);
        }
        if (Period.between(fecha, LocalDate.now()).getYears() < 18) {
            throw new ReglaNegocioException(ErrorMessage.CLIENTE_MENOR_DE_EDAD);
        }
    }

    private static String textoOpcional(String valor) {
        return valor != null && !valor.isBlank() ? valor.trim() : null;
    }

    private PerfilResponse aPerfil(String username, Cliente cliente) {
        Domicilio dom = cliente.getDomicilio();
        return PerfilResponse.builder()
                .username(username)
                .nombreCompleto(clienteMapper.construirNombreCompleto(cliente))
                .fechaNacimiento(cliente.getFechaNacimiento())
                .curp(cliente.getCurp())
                .rfc(cliente.getRfc())
                .correo(cliente.getCorreo())
                .telefonoMovil(cliente.getTelefonoMovil())
                .telefonoAlternativo(cliente.getTelefonoAlternativo())
                .ocupacion(cliente.getOcupacion() != null ? cliente.getOcupacion().getDescripcion() : null)
                .empresa(cliente.getEmpresa())
                .domicilio(clienteMapper.mapearADomicilioDTO(dom))
                .numeroCuenta(cliente.getCuenta() != null ? cliente.getCuenta().getNumeroCuenta() : null)
                .estatusCuenta(cliente.getCuenta() != null && cliente.getCuenta().getEstatus() != null
                        ? cliente.getCuenta().getEstatus().name() : null)
                .fechaRegistro(cliente.getFechaRegistro())
                .google2faHabilitado(cliente.getDatosSeguridad() != null
                        && Boolean.TRUE.equals(cliente.getDatosSeguridad().getTotpHabilitado()))
                .datosEditables(ActualizarPerfilRequest.builder()
                        .nombre(cliente.getNombre())
                        .segundoNombre(cliente.getSegundoNombre())
                        .apellidoPaterno(cliente.getApellidoPaterno())
                        .apellidoMaterno(cliente.getApellidoMaterno())
                        .fechaNacimiento(cliente.getFechaNacimiento())
                        .sexo(cliente.getSexo() != null ? cliente.getSexo().getCodigo() : null)
                        .nacionalidad(cliente.getNacionalidad() != null ? cliente.getNacionalidad().getCodigo() : null)
                        .estadoCivil(cliente.getEstadoCivil() != null ? cliente.getEstadoCivil().getCodigo() : null)
                        .correo(cliente.getCorreo())
                        .telefonoMovil(cliente.getTelefonoMovil())
                        .telefonoAlternativo(cliente.getTelefonoAlternativo())
                        .ocupacion(cliente.getOcupacion() != null ? cliente.getOcupacion().getCodigo() : null)
                        .empresa(cliente.getEmpresa())
                        .ingresoMensual(cliente.getIngresoMensual() != null
                                ? cliente.getIngresoMensual().setScale(2, RoundingMode.HALF_UP) : null)
                        .domicilio(dom == null ? null : DomicilioDTO.builder()
                                .calle(dom.getCalle())
                                .numeroExterior(dom.getNumeroExterior())
                                .numeroInterior(dom.getNumeroInterior())
                                .colonia(dom.getColonia())
                                .municipio(dom.getMunicipio())
                                .estado(dom.getEstado() != null ? dom.getEstado().getCodigo() : null)
                                .codigoPostal(dom.getCodigoPostal())
                                .pais(dom.getPais() != null ? dom.getPais().getCodigo() : null)
                                .build())
                        .build())
                .build();
    }
}
