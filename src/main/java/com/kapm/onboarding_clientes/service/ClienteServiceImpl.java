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
import java.util.function.Function;
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
    private final TokenService tokenService;

    private static final String TIPO_CURP = "CURP";
    private static final String TIPO_RFC = "RFC";

    public ClienteServiceImpl(
            ClienteRepository clienteRepository,
            CuentaService cuentaService,
            SeguridadBiometriaService seguridadBiometriaService,
            DatosSeguridadBiometriaRepository seguridadRepository,
            CatalogoService catalogoService,
            ClienteMapper clienteMapper,
            TokenService tokenService,
            @Value("${app.seguridad.inactividad-segundos:900}") long inactividadLimiteSegundos) {
        this.tokenService = tokenService;
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
                .numeroInterior(recortar(request.getDomicilio().getNumeroInterior()))
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
                .segundoNombre(recortar(request.getSegundoNombre()))
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
                .telefonoAlternativo(textoOpcional(request.getTelefonoAlternativo()))
                .ocupacion(ocupacionCat)
                .empresa(textoOpcional(request.getLaboral().getEmpresa()))
                .ingresoMensual(request.getLaboral().getIngresoMensual())
                .activo(true)
                .domicilio(domicilio)
                .cuenta(cuenta)
                .datosSeguridad(seguridad)
                .build();

        // El mapper nunca expone IDs de BD (cliente ni cuenta)
        return clienteMapper.mapearAClienteResponse(clienteRepository.save(cliente));
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
        return clienteMapper.mapearAClienteResponse(buscarPorCurp(curp));
    }

    @Override
    @Transactional
    public ClienteResponse obtenerClientePorRfc(String rfc) {
        return clienteMapper.mapearAClienteResponse(buscarPorRfc(rfc));
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
        return segunIdentificador(request.getCurp(), request.getRfc(),
                curp -> actualizarClientePorCurp(curp, request),
                rfc -> actualizarClientePorRfc(rfc, request));
    }

    @Override
    @Transactional
    public ClienteResponse actualizarClientePorCurp(String curp, ClienteUpdateRequest request) {
        return ejecutarActualizacion(buscarPorCurp(curp), request);
    }

    @Override
    @Transactional
    public ClienteResponse actualizarClientePorRfc(String rfc, ClienteUpdateRequest request) {
        return ejecutarActualizacion(buscarPorRfc(rfc), request);
    }

    private ClienteResponse ejecutarActualizacion(Cliente cliente, ClienteUpdateRequest request) {
        exigirActivo(cliente);
        asignarCorreoUnico(cliente, request.getCorreo());

        // Actualización de campos modificables únicamente (sexo, estado civil, teléfono, domicilio, laboral)
        if (request.getSexo() != null && !request.getSexo().isBlank()) {
            cliente.setSexo(catalogoService.buscarSexo(request.getSexo()));
        }

        if (request.getEstadoCivil() != null && !request.getEstadoCivil().isBlank()) {
            cliente.setEstadoCivil(catalogoService.buscarEstadoCivil(request.getEstadoCivil()));
        }

        cliente.setTelefonoMovil(request.getTelefonoMovil().trim());
        cliente.setTelefonoAlternativo(textoOpcional(request.getTelefonoAlternativo()));

        if (request.getDomicilio() != null) {
            Domicilio dom = cliente.getDomicilio();
            if (dom == null) {
                dom = new Domicilio();
                cliente.setDomicilio(dom);
            }
            DomicilioDTO nuevo = request.getDomicilio();
            dom.setEstado(catalogoService.buscarEstadoRepublica(nuevo.getEstado()));
            dom.setPais(catalogoService.buscarPais(nuevo.getPais()));
            dom.setCalle(nuevo.getCalle().trim());
            dom.setNumeroExterior(nuevo.getNumeroExterior().trim());
            dom.setNumeroInterior(recortar(nuevo.getNumeroInterior()));
            dom.setColonia(nuevo.getColonia().trim());
            dom.setMunicipio(nuevo.getMunicipio().trim());
            dom.setCodigoPostal(nuevo.getCodigoPostal().trim());
        }

        if (request.getLaboral() != null) {
            cliente.setOcupacion(catalogoService.buscarOcupacion(request.getLaboral().getOcupacion()));
            cliente.setEmpresa(textoOpcional(request.getLaboral().getEmpresa()));
            cliente.setIngresoMensual(request.getLaboral().getIngresoMensual());
        }

        // Ningún campo de identidad (nombre, segundoNombre, apellidos, fechaNacimiento, curp, rfc, nacionalidad)
        // es modificado bajo ninguna circunstancia.
        return clienteMapper.mapearAClienteResponse(clienteRepository.save(cliente));
    }

    private void desactivarEntidadCliente(Cliente cliente) {
        if (!Boolean.TRUE.equals(cliente.getActivo())) {
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
        desactivarEntidadCliente(segunIdentificador(request.getCurp(), request.getRfc(), this::buscarPorCurp, this::buscarPorRfc));
    }

    @Override
    @Transactional
    public void desactivarClientePorCurp(String curp) {
        desactivarEntidadCliente(buscarPorCurp(curp));
    }

    @Override
    @Transactional
    public void desactivarClientePorRfc(String rfc) {
        desactivarEntidadCliente(buscarPorRfc(rfc));
    }

    @Override
    @Transactional
    public ClienteResponse reactivarCliente(ClienteIdentificadorRequest request) {
        return segunIdentificador(request.getCurp(), request.getRfc(), this::reactivarClientePorCurp, this::reactivarClientePorRfc);
    }

    @Override
    @Transactional
    public ClienteResponse reactivarClientePorCurp(String curp) {
        return reactivar(buscarPorCurp(curp), CurpRfcUtil.sanitizarCurp(curp));
    }

    @Override
    @Transactional
    public ClienteResponse reactivarClientePorRfc(String rfc) {
        return reactivar(buscarPorRfc(rfc), CurpRfcUtil.sanitizarRfc(rfc));
    }

    private ClienteResponse reactivar(Cliente cliente, String identificador) {
        if (Boolean.TRUE.equals(cliente.getActivo())) {
            throw new ReglaNegocioException(ErrorMessage.CLIENTE_YA_ACTIVO, identificador);
        }

        cliente.setActivo(true);
        if (cliente.getCuenta() != null) {
            cliente.getCuenta().setEstatus(EstatusCuenta.ACTIVA);
        }

        return clienteMapper.mapearAClienteResponse(clienteRepository.save(cliente));
    }

    @Override
    @Transactional(readOnly = true)
    public ValidacionIdentificadorResponse validarDisponibilidad(ValidarIdentificadorRequest request) {
        return segunIdentificador(request.getCurp(), request.getRfc(), this::validarDisponibilidadCurp, this::validarDisponibilidadRfc);
    }

    @Override
    @Transactional(readOnly = true)
    public ValidacionIdentificadorResponse validarDisponibilidadCurp(String curp) {
        if (curp == null || curp.isBlank()) {
            return formatoInvalido(curp, TIPO_CURP, ErrorMessage.CURP_FORMATO_INVALIDO);
        }
        String curpLimpia = CurpRfcUtil.sanitizarCurp(curp);
        if (!CurpRfcUtil.esCurpValida(curpLimpia)) {
            return formatoInvalido(curpLimpia, TIPO_CURP, ErrorMessage.CURP_FORMATO_INVALIDO);
        }
        boolean existe = clienteRepository.existsByCurp(curpLimpia);
        return disponibilidad(curpLimpia, TIPO_CURP, existe,
                existe ? ErrorMessage.CURP_DUPLICADA.getMensaje() : "La CURP está disponible para registro");
    }

    @Override
    @Transactional(readOnly = true)
    public ValidacionIdentificadorResponse validarDisponibilidadRfc(String rfc) {
        if (rfc == null || rfc.isBlank()) {
            return formatoInvalido(rfc, TIPO_RFC, ErrorMessage.RFC_FORMATO_INVALIDO);
        }
        String rfcLimpio = CurpRfcUtil.sanitizarRfc(rfc);
        if (!CurpRfcUtil.esRfcValido(rfcLimpio)) {
            return formatoInvalido(rfcLimpio, TIPO_RFC, ErrorMessage.RFC_FORMATO_INVALIDO);
        }
        boolean existe = clienteRepository.existsByRfc(rfcLimpio);
        return disponibilidad(rfcLimpio, TIPO_RFC, existe,
                existe ? ErrorMessage.RFC_DUPLICADO.getMensaje() : "El RFC está disponible para registro");
    }

    private static ValidacionIdentificadorResponse formatoInvalido(String identificador, String tipo, ErrorMessage error) {
        return ValidacionIdentificadorResponse.builder()
                .identificador(identificador)
                .tipo(tipo)
                .formatoValido(false)
                .disponible(false)
                .mensaje(error.getMensaje())
                .build();
    }

    private static ValidacionIdentificadorResponse disponibilidad(String identificador, String tipo, boolean existe, String mensaje) {
        return ValidacionIdentificadorResponse.builder()
                .identificador(identificador)
                .tipo(tipo)
                .formatoValido(true)
                .disponible(!existe)
                .mensaje(mensaje)
                .build();
    }

    @Override
    @Transactional
    public ClienteResponse actualizarContacto(ActualizarContactoRequest request) {
        return segunIdentificador(request.getCurp(), request.getRfc(),
                curp -> actualizarContactoPorCurp(curp, request),
                rfc -> actualizarContactoPorRfc(rfc, request));
    }

    @Override
    @Transactional
    public ClienteResponse actualizarContactoPorCurp(String curp, ActualizarContactoRequest request) {
        return aplicarContacto(buscarPorCurp(curp), request);
    }

    @Override
    @Transactional
    public ClienteResponse actualizarContactoPorRfc(String rfc, ActualizarContactoRequest request) {
        return aplicarContacto(buscarPorRfc(rfc), request);
    }

    private ClienteResponse aplicarContacto(Cliente cliente, ActualizarContactoRequest request) {
        exigirActivo(cliente);
        asignarCorreoUnico(cliente, request.getCorreo());
        cliente.setTelefonoMovil(request.getTelefonoMovil().trim());
        cliente.setTelefonoAlternativo(textoOpcional(request.getTelefonoAlternativo()));
        return clienteMapper.mapearAClienteResponse(clienteRepository.save(cliente));
    }

    @Override
    @Transactional
    public CuentaResponse obtenerCuenta(ClienteIdentificadorRequest request) {
        return segunIdentificador(request.getCurp(), request.getRfc(), this::obtenerCuentaPorCurp, this::obtenerCuentaPorRfc);
    }

    @Override
    @Transactional
    public CuentaResponse obtenerCuentaPorCurp(String curp) {
        return cuentaDe(buscarPorCurp(curp));
    }

    @Override
    @Transactional
    public CuentaResponse obtenerCuentaPorRfc(String rfc) {
        return cuentaDe(buscarPorRfc(rfc));
    }

    private CuentaResponse cuentaDe(Cliente cliente) {
        if (cliente.getCuenta() == null) {
            throw new RecursoNoEncontradoException(ErrorMessage.CLIENTE_SIN_CUENTA);
        }
        return cuentaService.obtenerCuentaPorNumero(cliente.getCuenta().getNumeroCuenta());
    }

    @Override
    @Transactional(readOnly = true)
    public String obtenerCurpPorUsername(String username) {
        return clienteRepository.findByDatosSeguridadUsername(username)
                .map(Cliente::getCurp)
                .orElseThrow(AccesoDenegadoException::new);
    }

    @Override
    @Transactional
    public LoginResponse autenticarCliente(LoginRequest request) {
        CredencialesValidas validas = verificar(request);
        return emitirSesion(validas.seguridad(), validas.cliente(), "Autenticación exitosa mediante " + validas.metodo());
    }

    @Override
    @Transactional
    public Cliente verificarCredenciales(LoginRequest request) {
        return verificar(request).cliente();
    }

    @Override
    @Transactional
    public LoginResponse abrirSesion(String username) {
        DatosSeguridadBiometria seguridad = seguridadRepository.findByUsername(username)
                .orElseThrow(() -> new ReglaNegocioException(ErrorMessage.AUTH_CREDENCIALES_INVALIDAS));
        Cliente cliente = clienteRepository.findByDatosSeguridadUsername(username).orElse(null);
        if (cliente != null && !cliente.getActivo()) {
            throw new ReglaNegocioException(ErrorMessage.AUTH_CLIENTE_INACTIVO);
        }
        return emitirSesion(seguridad, cliente, "Autenticación exitosa con verificación por correo");
    }

    private record CredencialesValidas(DatosSeguridadBiometria seguridad, Cliente cliente, String metodo) {
    }

    private CredencialesValidas verificar(LoginRequest request) {
        // Usuario inexistente y credencial incorrecta responden igual: no se revela qué usernames existen
        DatosSeguridadBiometria seguridad = seguridadRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new ReglaNegocioException(ErrorMessage.AUTH_CREDENCIALES_INVALIDAS));

        Cliente cliente = clienteRepository.findByDatosSeguridadUsername(seguridad.getUsername()).orElse(null);

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
            seguridad.setSesionId(null);
            seguridadRepository.save(seguridad);
            throw new ReglaNegocioException(ErrorMessage.AUTH_CREDENCIALES_INVALIDAS);
        }
        return new CredencialesValidas(seguridad, cliente, metodoAuth);
    }

    private LoginResponse emitirSesion(DatosSeguridadBiometria seguridad, Cliente cliente, String mensaje) {
        // Nueva sesión: cualquier token emitido antes deja de ser válido
        String sesionId = UUID.randomUUID().toString();
        seguridad.setLoggeado(true);
        seguridad.setSesionId(sesionId);
        seguridad.setUltimaActividad(LocalDateTime.now());
        seguridadRepository.save(seguridad);

        TokenService.TokenEmitido token = tokenService.emitir(seguridad.getUsername(), sesionId);

        String nombreCompleto = cliente != null ? cliente.getNombre() + " " + cliente.getApellidoPaterno() : "Usuario Sistema";
        String numeroCuenta = cliente != null && cliente.getCuenta() != null ? cliente.getCuenta().getNumeroCuenta() : "N/A";

        return LoginResponse.builder()
                .autenticado(true)
                .loggeado(true)
                .mensaje(mensaje)
                .username(seguridad.getUsername())
                .clienteId(null)
                .clienteNombreCompleto(nombreCompleto)
                .numeroCuenta(numeroCuenta)
                .curp(cliente != null ? cliente.getCurp() : null)
                .rfc(cliente != null ? cliente.getRfc() : null)
                .tipoBiometriaValidada(seguridad.getTipoBiometria())
                .accessToken(token.valor())
                .tokenType("Bearer")
                .expiraEn(token.expiraEn())
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

    /** Opera por CURP si viene informada; si no, por RFC. Sin ninguno de los dos responde regla de negocio. */
    private static <T> T segunIdentificador(String curp, String rfc, Function<String, T> porCurp, Function<String, T> porRfc) {
        if (curp != null && !curp.isBlank()) {
            return porCurp.apply(curp);
        }
        if (rfc != null && !rfc.isBlank()) {
            return porRfc.apply(rfc);
        }
        throw new ReglaNegocioException(ErrorMessage.IDENTIFICADOR_REQUERIDO);
    }

    private Cliente buscarPorCurp(String curp) {
        if (!CurpRfcUtil.esCurpValida(curp)) {
            throw new ReglaNegocioException(ErrorMessage.CURP_FORMATO_INVALIDO, curp);
        }
        String curpLimpia = CurpRfcUtil.sanitizarCurp(curp);
        return clienteRepository.findByCurp(curpLimpia)
                .orElseThrow(() -> new ClienteNoEncontradoException(ErrorMessage.CLIENTE_NO_ENCONTRADO, curpLimpia));
    }

    private Cliente buscarPorRfc(String rfc) {
        if (!CurpRfcUtil.esRfcValido(rfc)) {
            throw new ReglaNegocioException(ErrorMessage.RFC_FORMATO_INVALIDO, rfc);
        }
        String rfcLimpio = CurpRfcUtil.sanitizarRfc(rfc);
        return clienteRepository.findByRfc(rfcLimpio)
                .orElseThrow(() -> new ClienteNoEncontradoException(ErrorMessage.CLIENTE_NO_ENCONTRADO, rfcLimpio));
    }

    private static void exigirActivo(Cliente cliente) {
        if (!Boolean.TRUE.equals(cliente.getActivo())) {
            throw new ReglaNegocioException(ErrorMessage.CLIENTE_INACTIVO);
        }
    }

    /** Asigna el correo normalizado validando que no pertenezca a otro cliente. */
    private void asignarCorreoUnico(Cliente cliente, String correo) {
        String nuevoCorreo = correo.trim().toLowerCase();
        if (!nuevoCorreo.equalsIgnoreCase(cliente.getCorreo()) && clienteRepository.existsByCorreoAndIdNot(nuevoCorreo, cliente.getId())) {
            throw new CorreoDuplicadoException(ErrorMessage.CORREO_DUPLICADO, nuevoCorreo);
        }
        cliente.setCorreo(nuevoCorreo);
    }

    private static String recortar(String valor) {
        return valor != null ? valor.trim() : null;
    }

    /** Campo opcional: vacío o en blanco se guarda como null. */
    private static String textoOpcional(String valor) {
        return valor != null && !valor.isBlank() ? valor.trim() : null;
    }
}
