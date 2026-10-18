package com.kapm.onboarding_clientes.service;

import com.kapm.onboarding_clientes.dto.request.*;
import com.kapm.onboarding_clientes.dto.response.ClienteResponse;
import com.kapm.onboarding_clientes.dto.response.CuentaResponse;
import com.kapm.onboarding_clientes.dto.response.LoginResponse;
import com.kapm.onboarding_clientes.dto.response.PageResponse;
import com.kapm.onboarding_clientes.dto.response.ValidacionIdentificadorResponse;
import com.kapm.onboarding_clientes.entity.*;
import com.kapm.onboarding_clientes.entity.catalogo.*;
import com.kapm.onboarding_clientes.exception.*;
import com.kapm.onboarding_clientes.mapper.ClienteMapper;
import com.kapm.onboarding_clientes.repository.ClienteRepository;
import com.kapm.onboarding_clientes.repository.DatosSeguridadBiometriaRepository;
import com.kapm.onboarding_clientes.repository.specification.ClienteSpecification;
import com.kapm.onboarding_clientes.validation.CurpRfcUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ClienteServiceImpl implements ClienteService {

    private final ClienteRepository clienteRepository;
    private final CuentaService cuentaService;
    private final SeguridadBiometriaService seguridadBiometriaService;
    private final DatosSeguridadBiometriaRepository seguridadRepository;
    private final CatalogoService catalogoService;
    private final ClienteMapper clienteMapper;
    private final long inactividadLimiteSegundos;

    public ClienteServiceImpl(
            ClienteRepository clienteRepository,
            CuentaService cuentaService,
            SeguridadBiometriaService seguridadBiometriaService,
            DatosSeguridadBiometriaRepository seguridadRepository,
            CatalogoService catalogoService,
            ClienteMapper clienteMapper,
            @Value("${app.seguridad.inactividad-segundos:5}") long inactividadLimiteSegundos) {
        this.clienteRepository = clienteRepository;
        this.cuentaService = cuentaService;
        this.seguridadBiometriaService = seguridadBiometriaService;
        this.seguridadRepository = seguridadRepository;
        this.catalogoService = catalogoService;
        this.clienteMapper = clienteMapper;
        this.inactividadLimiteSegundos = inactividadLimiteSegundos > 0 ? inactividadLimiteSegundos : SeguridadBiometriaServiceImpl.INACTIVITY_TIMEOUT_SECONDS;
    }

    @Override
    @Transactional
    public ClienteResponse registrarCliente(ClienteRegistrationRequest request) {

        validarMayoriaDeEdad(request.getFechaNacimiento());

        String curp = CurpRfcUtil.sanitizarCurp(request.getCurp());
        if (!CurpRfcUtil.esCurpValida(curp)) {
            throw new ReglaNegocioException(ErrorMessage.CURP_FORMATO_INVALIDO, request.getCurp());
        }
        if (clienteRepository.existsByCurp(curp)) {
            throw new CurpDuplicadaException(ErrorMessage.CURP_DUPLICADA, curp);
        }

        String rfc = CurpRfcUtil.sanitizarRfc(request.getRfc());
        if (!CurpRfcUtil.esRfcValido(rfc)) {
            throw new ReglaNegocioException(ErrorMessage.RFC_FORMATO_INVALIDO, request.getRfc());
        }
        if (clienteRepository.existsByRfc(rfc)) {
            throw new RfcDuplicadoException(ErrorMessage.RFC_DUPLICADO, rfc);
        }

        String correo = request.getCorreo().trim().toLowerCase();
        if (clienteRepository.existsByCorreo(correo)) {
            throw new CorreoDuplicadoException(ErrorMessage.CORREO_DUPLICADO, correo);
        }

        CatSexo sexoCat = catalogoService.buscarSexo(request.getSexo());
        CatEstadoCivil estadoCivilCat = catalogoService.buscarEstadoCivil(request.getEstadoCivil());
        CatNacionalidad nacionalidadCat = catalogoService.buscarNacionalidad(request.getNacionalidad());
        CatOcupacion ocupacionCat = catalogoService.buscarOcupacion(request.getLaboral().getOcupacion());
        CatEstadoRepublica estadoDomicilioCat = catalogoService.buscarEstadoRepublica(request.getDomicilio().getEstado());
        CatPais paisDomicilioCat = catalogoService.buscarPais(request.getDomicilio().getPais());

        Domicilio domicilio = Domicilio.builder()
                .calle(request.getDomicilio().getCalle().trim())
                .numeroExterior(request.getDomicilio().getNumeroExterior().trim())
                .numeroInterior(request.getDomicilio().getNumeroInterior() != null ? request.getDomicilio().getNumeroInterior().trim() : null)
                .colonia(request.getDomicilio().getColonia().trim())
                .municipio(request.getDomicilio().getMunicipio().trim())
                .estado(estadoDomicilioCat)
                .codigoPostal(request.getDomicilio().getCodigoPostal().trim())
                .pais(paisDomicilioCat)
                .build();

        Cuenta cuenta = cuentaService.crearCuentaInicial(request.getSaldoInicial());
        DatosSeguridadBiometria seguridad = crearOPrepararSeguridad(request, curp);

        Cliente cliente = Cliente.builder()
                .nombre(request.getNombre().trim())
                .segundoNombre(request.getSegundoNombre() != null ? request.getSegundoNombre().trim() : null)
                .apellidoPaterno(request.getApellidoPaterno().trim())
                .apellidoMaterno(request.getApellidoMaterno().trim())
                .fechaNacimiento(request.getFechaNacimiento())
                .curp(curp)
                .rfc(rfc)
                .sexo(sexoCat)
                .nacionalidad(nacionalidadCat)
                .estadoCivil(estadoCivilCat)
                .correo(correo)
                .telefonoMovil(request.getTelefonoMovil().trim())
                .telefonoAlternativo(request.getTelefonoAlternativo() != null && !request.getTelefonoAlternativo().isBlank() ? request.getTelefonoAlternativo().trim() : null)
                .ocupacion(ocupacionCat)
                .empresa(request.getLaboral().getEmpresa().trim())
                .ingresoMensual(request.getLaboral().getIngresoMensual())
                .activo(true)
                .domicilio(domicilio)
                .cuenta(cuenta)
                .datosSeguridad(seguridad)
                .build();

        Cliente clienteGuardado = clienteRepository.save(cliente);
        ClienteResponse response = clienteMapper.mapearAClienteResponse(clienteGuardado);

        response.setId(null);
        if (response.getCuenta() != null) {
            response.getCuenta().setId(null);
        }

        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClienteResponse> obtenerTodosLosClientes() {
        return clienteRepository.findAll().stream()
                .map(clienteMapper::mapearAClienteResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClienteResponse> obtenerClientesActivos() {
        return clienteRepository.findByActivoTrue().stream()
                .map(clienteMapper::mapearAClienteResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ClienteResponse> buscarClientes(BuscarClienteRequest request) {
        Specification<Cliente> spec = ClienteSpecification.conFiltros(request);
        Pageable pageable = PageRequest.of(request.getPaginaSegura(), request.getTamanioSeguro());
        Page<Cliente> page = clienteRepository.findAll(spec, pageable);

        if (page.isEmpty()) {
            throw new ClienteNoEncontradoException(ErrorMessage.CLIENTE_NO_ENCONTRADO_BUSQUEDA);
        }

        Page<ClienteResponse> dtoPage = page.map(clienteMapper::mapearAClienteResponse);
        return PageResponse.from(dtoPage);
    }

    @Override
    @Transactional
    public ClienteResponse obtenerClientePorCurp(String curp) {
        if (!CurpRfcUtil.esCurpValida(curp)) {
            throw new ReglaNegocioException(ErrorMessage.CURP_FORMATO_INVALIDO, curp);
        }
        String curpLimpia = CurpRfcUtil.sanitizarCurp(curp);
        Cliente cliente = clienteRepository.findByCurp(curpLimpia)
                .orElseThrow(() -> new ClienteNoEncontradoException(ErrorMessage.CLIENTE_NO_ENCONTRADO, curpLimpia));
        return clienteMapper.mapearAClienteResponse(cliente);
    }

    @Override
    @Transactional
    public ClienteResponse obtenerClientePorRfc(String rfc) {
        if (!CurpRfcUtil.esRfcValido(rfc)) {
            throw new ReglaNegocioException(ErrorMessage.RFC_FORMATO_INVALIDO, rfc);
        }
        String rfcLimpio = CurpRfcUtil.sanitizarRfc(rfc);
        Cliente cliente = clienteRepository.findByRfc(rfcLimpio)
                .orElseThrow(() -> new ClienteNoEncontradoException(ErrorMessage.CLIENTE_NO_ENCONTRADO, rfcLimpio));
        return clienteMapper.mapearAClienteResponse(cliente);
    }

    @Override
    @Transactional
    public ClienteResponse obtenerClientePorCorreo(String correo) {
        Cliente cliente = clienteRepository.findByCorreo(correo.trim().toLowerCase())
                .orElseThrow(() -> new ClienteNoEncontradoException(ErrorMessage.CLIENTE_NO_ENCONTRADO, correo));
        return clienteMapper.mapearAClienteResponse(cliente);
    }

    @Override
    @Transactional
    public ClienteResponse obtenerClientePorNumeroCuenta(String numeroCuenta) {
        Cliente cliente = clienteRepository.findByCuentaNumeroCuenta(numeroCuenta)
                .orElseThrow(() -> new ClienteNoEncontradoException(ErrorMessage.CLIENTE_NO_ENCONTRADO, numeroCuenta));
        return clienteMapper.mapearAClienteResponse(cliente);
    }

    @Override
    @Transactional
    public ClienteResponse actualizarClientePorCurp(ActualizarPorCurpRequest request) {
        ClienteUpdateRequest updateReq = ClienteUpdateRequest.builder()
                .curp(request.getCurp())
                .sexo(request.getSexo())
                .estadoCivil(request.getEstadoCivil())
                .correo(request.getCorreo())
                .telefonoMovil(request.getTelefonoMovil())
                .telefonoAlternativo(request.getTelefonoAlternativo())
                .domicilio(request.getDomicilio())
                .laboral(request.getLaboral())
                .build();
        return actualizarClientePorCurp(request.getCurp(), updateReq);
    }

    @Override
    @Transactional
    public ClienteResponse actualizarClientePorRfc(ActualizarPorRfcRequest request) {
        ClienteUpdateRequest updateReq = ClienteUpdateRequest.builder()
                .rfc(request.getRfc())
                .sexo(request.getSexo())
                .estadoCivil(request.getEstadoCivil())
                .correo(request.getCorreo())
                .telefonoMovil(request.getTelefonoMovil())
                .telefonoAlternativo(request.getTelefonoAlternativo())
                .domicilio(request.getDomicilio())
                .laboral(request.getLaboral())
                .build();
        return actualizarClientePorRfc(request.getRfc(), updateReq);
    }

    @Override
    @Transactional
    public ClienteResponse actualizarCliente(ClienteUpdateRequest request) {
        if (request.getCurp() != null && !request.getCurp().isBlank()) {
            return actualizarClientePorCurp(request.getCurp(), request);
        } else if (request.getRfc() != null && !request.getRfc().isBlank()) {
            return actualizarClientePorRfc(request.getRfc(), request);
        } else {
            throw new ReglaNegocioException(ErrorMessage.IDENTIFICADOR_REQUERIDO);
        }
    }

    @Override
    @Transactional
    public ClienteResponse actualizarClientePorCurp(String curp, ClienteUpdateRequest request) {
        if (!CurpRfcUtil.esCurpValida(curp)) {
            throw new ReglaNegocioException(ErrorMessage.CURP_FORMATO_INVALIDO, curp);
        }
        String curpLimpia = CurpRfcUtil.sanitizarCurp(curp);
        Cliente cliente = clienteRepository.findByCurp(curpLimpia)
                .orElseThrow(() -> new ClienteNoEncontradoException(ErrorMessage.CLIENTE_NO_ENCONTRADO, curpLimpia));
        return ejecutarActualizacion(cliente, request);
    }

    @Override
    @Transactional
    public ClienteResponse actualizarClientePorRfc(String rfc, ClienteUpdateRequest request) {
        if (!CurpRfcUtil.esRfcValido(rfc)) {
            throw new ReglaNegocioException(ErrorMessage.RFC_FORMATO_INVALIDO, rfc);
        }
        String rfcLimpio = CurpRfcUtil.sanitizarRfc(rfc);
        Cliente cliente = clienteRepository.findByRfc(rfcLimpio)
                .orElseThrow(() -> new ClienteNoEncontradoException(ErrorMessage.CLIENTE_NO_ENCONTRADO, rfcLimpio));
        return ejecutarActualizacion(cliente, request);
    }

    private ClienteResponse ejecutarActualizacion(Cliente cliente, ClienteUpdateRequest request) {
        if (!Boolean.TRUE.equals(cliente.getActivo())) {
            throw new ReglaNegocioException(ErrorMessage.CLIENTE_INACTIVO);
        }

        // Validación de unicidad de correo excluyendo al cliente actual
        String nuevoCorreo = request.getCorreo().trim().toLowerCase();
        if (!nuevoCorreo.equalsIgnoreCase(cliente.getCorreo()) && clienteRepository.existsByCorreoAndIdNot(nuevoCorreo, cliente.getId())) {
            throw new CorreoDuplicadoException(ErrorMessage.CORREO_DUPLICADO, nuevoCorreo);
        }
        cliente.setCorreo(nuevoCorreo);

        // Actualización de campos modificables únicamente (sexo, estado civil, teléfono, domicilio, laboral)
        if (request.getSexo() != null && !request.getSexo().isBlank()) {
            CatSexo sexoCat = catalogoService.buscarSexo(request.getSexo());
            cliente.setSexo(sexoCat);
        }

        if (request.getEstadoCivil() != null && !request.getEstadoCivil().isBlank()) {
            CatEstadoCivil estadoCivilCat = catalogoService.buscarEstadoCivil(request.getEstadoCivil());
            cliente.setEstadoCivil(estadoCivilCat);
        }

        cliente.setTelefonoMovil(request.getTelefonoMovil().trim());
        cliente.setTelefonoAlternativo(request.getTelefonoAlternativo() != null && !request.getTelefonoAlternativo().isBlank() 
                ? request.getTelefonoAlternativo().trim() 
                : null);

        if (request.getDomicilio() != null) {
            Domicilio dom = cliente.getDomicilio();
            if (dom == null) {
                dom = new Domicilio();
                cliente.setDomicilio(dom);
            }
            CatEstadoRepublica estadoDomicilioCat = catalogoService.buscarEstadoRepublica(request.getDomicilio().getEstado());
            CatPais paisDomicilioCat = catalogoService.buscarPais(request.getDomicilio().getPais());

            dom.setCalle(request.getDomicilio().getCalle().trim());
            dom.setNumeroExterior(request.getDomicilio().getNumeroExterior().trim());
            dom.setNumeroInterior(request.getDomicilio().getNumeroInterior() != null ? request.getDomicilio().getNumeroInterior().trim() : null);
            dom.setColonia(request.getDomicilio().getColonia().trim());
            dom.setMunicipio(request.getDomicilio().getMunicipio().trim());
            dom.setEstado(estadoDomicilioCat);
            dom.setCodigoPostal(request.getDomicilio().getCodigoPostal().trim());
            dom.setPais(paisDomicilioCat);
        }

        if (request.getLaboral() != null) {
            CatOcupacion ocupacionCat = catalogoService.buscarOcupacion(request.getLaboral().getOcupacion());
            cliente.setOcupacion(ocupacionCat);
            cliente.setEmpresa(request.getLaboral().getEmpresa().trim());
            cliente.setIngresoMensual(request.getLaboral().getIngresoMensual());
        }

        // Ningún campo de identidad (nombre, segundoNombre, apellidos, fechaNacimiento, curp, rfc, nacionalidad)
        // es modificado bajo ninguna circunstancia.
        Cliente clienteActualizado = clienteRepository.save(cliente);
        return clienteMapper.mapearAClienteResponse(clienteActualizado);
    }

    private void desactivarEntidadCliente(Cliente cliente) {
        if (!cliente.getActivo()) {
            throw new ReglaNegocioException(ErrorMessage.CLIENTE_YA_INACTIVO);
        }

        cliente.setActivo(false);

        if (cliente.getCuenta() != null) {
            cliente.getCuenta().setEstatus(EstatusCuenta.INACTIVA);
        }

        if (cliente.getDatosSeguridad() != null) {
            cliente.getDatosSeguridad().setLoggeado(false);
        }

        clienteRepository.save(cliente);
    }

    @Override
    @Transactional
    public void desactivarCliente(ClienteIdentificadorRequest request) {
        if (request.getCurp() != null && !request.getCurp().isBlank()) {
            desactivarClientePorCurp(request.getCurp());
        } else if (request.getRfc() != null && !request.getRfc().isBlank()) {
            desactivarClientePorRfc(request.getRfc());
        } else {
            throw new ReglaNegocioException(ErrorMessage.IDENTIFICADOR_REQUERIDO);
        }
    }

    @Override
    @Transactional
    public void desactivarClientePorCurp(String curp) {
        if (!CurpRfcUtil.esCurpValida(curp)) {
            throw new ReglaNegocioException(ErrorMessage.CURP_FORMATO_INVALIDO, curp);
        }
        String curpLimpia = CurpRfcUtil.sanitizarCurp(curp);
        Cliente cliente = clienteRepository.findByCurp(curpLimpia)
                .orElseThrow(() -> new ClienteNoEncontradoException(ErrorMessage.CLIENTE_NO_ENCONTRADO, curpLimpia));
        desactivarEntidadCliente(cliente);
    }

    @Override
    @Transactional
    public void desactivarClientePorRfc(String rfc) {
        if (!CurpRfcUtil.esRfcValido(rfc)) {
            throw new ReglaNegocioException(ErrorMessage.RFC_FORMATO_INVALIDO, rfc);
        }
        String rfcLimpio = CurpRfcUtil.sanitizarRfc(rfc);
        Cliente cliente = clienteRepository.findByRfc(rfcLimpio)
                .orElseThrow(() -> new ClienteNoEncontradoException(ErrorMessage.CLIENTE_NO_ENCONTRADO, rfcLimpio));
        desactivarEntidadCliente(cliente);
    }

    @Override
    @Transactional
    public ClienteResponse reactivarCliente(ClienteIdentificadorRequest request) {
        if (request.getCurp() != null && !request.getCurp().isBlank()) {
            return reactivarClientePorCurp(request.getCurp());
        } else if (request.getRfc() != null && !request.getRfc().isBlank()) {
            return reactivarClientePorRfc(request.getRfc());
        } else {
            throw new ReglaNegocioException(ErrorMessage.IDENTIFICADOR_REQUERIDO);
        }
    }

    @Override
    @Transactional
    public ClienteResponse reactivarClientePorCurp(String curp) {
        if (!CurpRfcUtil.esCurpValida(curp)) {
            throw new ReglaNegocioException(ErrorMessage.CURP_FORMATO_INVALIDO, curp);
        }
        String curpLimpia = CurpRfcUtil.sanitizarCurp(curp);
        Cliente cliente = clienteRepository.findByCurp(curpLimpia)
                .orElseThrow(() -> new ClienteNoEncontradoException(ErrorMessage.CLIENTE_NO_ENCONTRADO, curpLimpia));

        if (Boolean.TRUE.equals(cliente.getActivo())) {
            throw new ReglaNegocioException(ErrorMessage.CLIENTE_YA_ACTIVO, curpLimpia);
        }

        cliente.setActivo(true);
        if (cliente.getCuenta() != null) {
            cliente.getCuenta().setEstatus(EstatusCuenta.ACTIVA);
        }

        Cliente clienteGuardado = clienteRepository.save(cliente);
        return clienteMapper.mapearAClienteResponse(clienteGuardado);
    }

    @Override
    @Transactional
    public ClienteResponse reactivarClientePorRfc(String rfc) {
        if (!CurpRfcUtil.esRfcValido(rfc)) {
            throw new ReglaNegocioException(ErrorMessage.RFC_FORMATO_INVALIDO, rfc);
        }
        String rfcLimpio = CurpRfcUtil.sanitizarRfc(rfc);
        Cliente cliente = clienteRepository.findByRfc(rfcLimpio)
                .orElseThrow(() -> new ClienteNoEncontradoException(ErrorMessage.CLIENTE_NO_ENCONTRADO, rfcLimpio));

        if (Boolean.TRUE.equals(cliente.getActivo())) {
            throw new ReglaNegocioException(ErrorMessage.CLIENTE_YA_ACTIVO, rfcLimpio);
        }

        cliente.setActivo(true);
        if (cliente.getCuenta() != null) {
            cliente.getCuenta().setEstatus(EstatusCuenta.ACTIVA);
        }

        Cliente clienteGuardado = clienteRepository.save(cliente);
        return clienteMapper.mapearAClienteResponse(clienteGuardado);
    }

    @Override
    @Transactional(readOnly = true)
    public ValidacionIdentificadorResponse validarDisponibilidad(ValidarIdentificadorRequest request) {
        if (request.getCurp() != null && !request.getCurp().isBlank()) {
            return validarDisponibilidadCurp(request.getCurp());
        } else if (request.getRfc() != null && !request.getRfc().isBlank()) {
            return validarDisponibilidadRfc(request.getRfc());
        } else {
            throw new ReglaNegocioException(ErrorMessage.IDENTIFICADOR_REQUERIDO);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ValidacionIdentificadorResponse validarDisponibilidadCurp(String curp) {
        if (curp == null || curp.isBlank()) {
            return ValidacionIdentificadorResponse.builder()
                    .identificador(curp)
                    .tipo("CURP")
                    .formatoValido(false)
                    .disponible(false)
                    .mensaje(ErrorMessage.CURP_FORMATO_INVALIDO.getMensaje())
                    .build();
        }

        String curpLimpia = CurpRfcUtil.sanitizarCurp(curp);
        boolean formatoValido = CurpRfcUtil.esCurpValida(curpLimpia);
        if (!formatoValido) {
            return ValidacionIdentificadorResponse.builder()
                    .identificador(curpLimpia)
                    .tipo("CURP")
                    .formatoValido(false)
                    .disponible(false)
                    .mensaje(ErrorMessage.CURP_FORMATO_INVALIDO.getMensaje())
                    .build();
        }

        boolean existe = clienteRepository.existsByCurp(curpLimpia);
        return ValidacionIdentificadorResponse.builder()
                .identificador(curpLimpia)
                .tipo("CURP")
                .formatoValido(true)
                .disponible(!existe)
                .mensaje(existe ? ErrorMessage.CURP_DUPLICADA.getMensaje() : "La CURP está disponible para registro")
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public ValidacionIdentificadorResponse validarDisponibilidadRfc(String rfc) {
        if (rfc == null || rfc.isBlank()) {
            return ValidacionIdentificadorResponse.builder()
                    .identificador(rfc)
                    .tipo("RFC")
                    .formatoValido(false)
                    .disponible(false)
                    .mensaje(ErrorMessage.RFC_FORMATO_INVALIDO.getMensaje())
                    .build();
        }

        String rfcLimpio = CurpRfcUtil.sanitizarRfc(rfc);
        boolean formatoValido = CurpRfcUtil.esRfcValido(rfcLimpio);
        if (!formatoValido) {
            return ValidacionIdentificadorResponse.builder()
                    .identificador(rfcLimpio)
                    .tipo("RFC")
                    .formatoValido(false)
                    .disponible(false)
                    .mensaje(ErrorMessage.RFC_FORMATO_INVALIDO.getMensaje())
                    .build();
        }

        boolean existe = clienteRepository.existsByRfc(rfcLimpio);
        return ValidacionIdentificadorResponse.builder()
                .identificador(rfcLimpio)
                .tipo("RFC")
                .formatoValido(true)
                .disponible(!existe)
                .mensaje(existe ? ErrorMessage.RFC_DUPLICADO.getMensaje() : "El RFC está disponible para registro")
                .build();
    }

    @Override
    @Transactional
    public ClienteResponse actualizarContacto(ActualizarContactoRequest request) {
        if (request.getCurp() != null && !request.getCurp().isBlank()) {
            return actualizarContactoPorCurp(request.getCurp(), request);
        } else if (request.getRfc() != null && !request.getRfc().isBlank()) {
            return actualizarContactoPorRfc(request.getRfc(), request);
        } else {
            throw new ReglaNegocioException(ErrorMessage.IDENTIFICADOR_REQUERIDO);
        }
    }

    @Override
    @Transactional
    public ClienteResponse actualizarContactoPorCurp(String curp, ActualizarContactoRequest request) {
        if (!CurpRfcUtil.esCurpValida(curp)) {
            throw new ReglaNegocioException(ErrorMessage.CURP_FORMATO_INVALIDO, curp);
        }
        String curpLimpia = CurpRfcUtil.sanitizarCurp(curp);
        Cliente cliente = clienteRepository.findByCurp(curpLimpia)
                .orElseThrow(() -> new ClienteNoEncontradoException(ErrorMessage.CLIENTE_NO_ENCONTRADO, curpLimpia));

        if (!cliente.getActivo()) {
            throw new ReglaNegocioException(ErrorMessage.CLIENTE_INACTIVO);
        }

        String nuevoCorreo = request.getCorreo().trim().toLowerCase();
        if (!nuevoCorreo.equalsIgnoreCase(cliente.getCorreo()) && clienteRepository.existsByCorreoAndIdNot(nuevoCorreo, cliente.getId())) {
            throw new CorreoDuplicadoException(ErrorMessage.CORREO_DUPLICADO, nuevoCorreo);
        }

        cliente.setCorreo(nuevoCorreo);
        cliente.setTelefonoMovil(request.getTelefonoMovil().trim());
        cliente.setTelefonoAlternativo(request.getTelefonoAlternativo() != null && !request.getTelefonoAlternativo().isBlank() ? request.getTelefonoAlternativo().trim() : null);

        Cliente guardado = clienteRepository.save(cliente);
        return clienteMapper.mapearAClienteResponse(guardado);
    }

    @Override
    @Transactional
    public ClienteResponse actualizarContactoPorRfc(String rfc, ActualizarContactoRequest request) {
        if (!CurpRfcUtil.esRfcValido(rfc)) {
            throw new ReglaNegocioException(ErrorMessage.RFC_FORMATO_INVALIDO, rfc);
        }
        String rfcLimpio = CurpRfcUtil.sanitizarRfc(rfc);
        Cliente cliente = clienteRepository.findByRfc(rfcLimpio)
                .orElseThrow(() -> new ClienteNoEncontradoException(ErrorMessage.CLIENTE_NO_ENCONTRADO, rfcLimpio));

        if (!cliente.getActivo()) {
            throw new ReglaNegocioException(ErrorMessage.CLIENTE_INACTIVO);
        }

        String nuevoCorreo = request.getCorreo().trim().toLowerCase();
        if (!nuevoCorreo.equalsIgnoreCase(cliente.getCorreo()) && clienteRepository.existsByCorreoAndIdNot(nuevoCorreo, cliente.getId())) {
            throw new CorreoDuplicadoException(ErrorMessage.CORREO_DUPLICADO, nuevoCorreo);
        }

        cliente.setCorreo(nuevoCorreo);
        cliente.setTelefonoMovil(request.getTelefonoMovil().trim());
        cliente.setTelefonoAlternativo(request.getTelefonoAlternativo() != null && !request.getTelefonoAlternativo().isBlank() ? request.getTelefonoAlternativo().trim() : null);

        Cliente guardado = clienteRepository.save(cliente);
        return clienteMapper.mapearAClienteResponse(guardado);
    }

    @Override
    @Transactional
    public CuentaResponse obtenerCuenta(ClienteIdentificadorRequest request) {
        if (request.getCurp() != null && !request.getCurp().isBlank()) {
            return obtenerCuentaPorCurp(request.getCurp());
        } else if (request.getRfc() != null && !request.getRfc().isBlank()) {
            return obtenerCuentaPorRfc(request.getRfc());
        } else {
            throw new ReglaNegocioException(ErrorMessage.IDENTIFICADOR_REQUERIDO);
        }
    }

    @Override
    @Transactional
    public CuentaResponse obtenerCuentaPorCurp(String curp) {
        if (!CurpRfcUtil.esCurpValida(curp)) {
            throw new ReglaNegocioException(ErrorMessage.CURP_FORMATO_INVALIDO, curp);
        }
        String curpLimpia = CurpRfcUtil.sanitizarCurp(curp);
        Cliente cliente = clienteRepository.findByCurp(curpLimpia)
                .orElseThrow(() -> new ClienteNoEncontradoException(ErrorMessage.CLIENTE_NO_ENCONTRADO, curpLimpia));

        if (cliente.getCuenta() == null) {
            throw new RecursoNoEncontradoException(ErrorMessage.CLIENTE_SIN_CUENTA);
        }

        return cuentaService.obtenerCuentaPorNumero(cliente.getCuenta().getNumeroCuenta());
    }

    @Override
    @Transactional
    public CuentaResponse obtenerCuentaPorRfc(String rfc) {
        if (!CurpRfcUtil.esRfcValido(rfc)) {
            throw new ReglaNegocioException(ErrorMessage.RFC_FORMATO_INVALIDO, rfc);
        }
        String rfcLimpio = CurpRfcUtil.sanitizarRfc(rfc);
        Cliente cliente = clienteRepository.findByRfc(rfcLimpio)
                .orElseThrow(() -> new ClienteNoEncontradoException(ErrorMessage.CLIENTE_NO_ENCONTRADO, rfcLimpio));

        if (cliente.getCuenta() == null) {
            throw new RecursoNoEncontradoException(ErrorMessage.CLIENTE_SIN_CUENTA);
        }

        return cuentaService.obtenerCuentaPorNumero(cliente.getCuenta().getNumeroCuenta());
    }

    @Override
    @Transactional
    public LoginResponse autenticarCliente(LoginRequest request) {
        DatosSeguridadBiometria seguridad = seguridadRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RecursoNoEncontradoException(ErrorMessage.AUTH_USUARIO_NO_ENCONTRADO, request.getUsername()));

        Cliente cliente = clienteRepository.findAll().stream()
                .filter(c -> c.getDatosSeguridad() != null && c.getDatosSeguridad().getId().equals(seguridad.getId()))
                .findFirst()
                .orElse(null);

        if (cliente != null && !cliente.getActivo()) {
            seguridad.setLoggeado(false);
            seguridadRepository.save(seguridad);
            throw new ReglaNegocioException(ErrorMessage.AUTH_CLIENTE_INACTIVO);
        }

        boolean autenticado = false;
        String metodoAuth = "Contraseña Cifrada";

        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            autenticado = seguridadBiometriaService.validarPassword(request.getPassword(), seguridad.getPasswordHash());
        }

        if (!autenticado && request.getDatosBiometricos() != null && !request.getDatosBiometricos().isBlank()) {
            autenticado = seguridadBiometriaService.validarBiometria(request.getDatosBiometricos(), seguridad.getHashBiometrico());
            metodoAuth = "Biometría (" + seguridad.getTipoBiometria() + ")";
        }

        if (!autenticado) {
            seguridad.setLoggeado(false);
            seguridadRepository.save(seguridad);
            throw new ReglaNegocioException(ErrorMessage.AUTH_CREDENCIALES_INVALIDAS);
        }

        seguridad.setLoggeado(true);
        seguridad.setUltimaActividad(LocalDateTime.now());
        seguridadRepository.save(seguridad);

        String nombreCompleto = cliente != null ? cliente.getNombre() + " " + cliente.getApellidoPaterno() : "Usuario Sistema";
        String numeroCuenta = cliente != null && cliente.getCuenta() != null ? cliente.getCuenta().getNumeroCuenta() : "N/A";

        return LoginResponse.builder()
                .autenticado(true)
                .loggeado(true)
                .mensaje("Autenticación exitosa mediante " + metodoAuth)
                .username(seguridad.getUsername())
                .clienteId(null)
                .clienteNombreCompleto(nombreCompleto)
                .numeroCuenta(numeroCuenta)
                .tipoBiometriaValidada(seguridad.getTipoBiometria())
                .tokenSimulado("SEC-TOKEN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .segundosDesdeUltimaActividad(0L)
                .estadoSesion("ACTIVA (Límite Inactividad: " + inactividadLimiteSegundos + " Segundos)")
                .timestamp(LocalDateTime.now())
                .build();
    }

    private void validarMayoriaDeEdad(LocalDate fechaNacimiento) {
        if (fechaNacimiento == null) {
            throw new ReglaNegocioException(ErrorMessage.FECHA_NACIMIENTO_REQUERIDA);
        }
        if (fechaNacimiento.isAfter(LocalDate.now())) {
            throw new ReglaNegocioException(ErrorMessage.FECHA_NACIMIENTO_FUTURA);
        }
        int edad = Period.between(fechaNacimiento, LocalDate.now()).getYears();
        if (edad < 18) {
            throw new ReglaNegocioException(ErrorMessage.CLIENTE_MENOR_DE_EDAD, "Edad calculada: " + edad + " años.");
        }
    }

    private DatosSeguridadBiometria crearOPrepararSeguridad(ClienteRegistrationRequest request, String curp) {
        if (request.getSeguridad() != null && request.getSeguridad().getUsername() != null && !request.getSeguridad().getUsername().isBlank()) {
            return seguridadBiometriaService.crearDatosSeguridad(
                    request.getSeguridad().getUsername().trim(),
                    request.getSeguridad().getPassword(),
                    request.getSeguridad().getTipoBiometria(),
                    request.getSeguridad().getDatosBiometricos()
            );
        } else {
            String usernameDefault = "usr_" + curp.toLowerCase().substring(0, 10);
            String passwordDefault = "Password123*";
            String biometriaDefault = "BIO-TEMPLATE-" + curp;
            return seguridadBiometriaService.crearDatosSeguridad(
                    usernameDefault,
                    passwordDefault,
                    TipoBiometria.HUELLA_DACTILAR,
                    biometriaDefault
            );
        }
    }
}
