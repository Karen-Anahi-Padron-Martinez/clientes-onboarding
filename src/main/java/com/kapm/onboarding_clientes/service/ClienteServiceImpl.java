package com.kapm.onboarding_clientes.service;

import com.kapm.onboarding_clientes.dto.request.ClienteRegistrationRequest;
import com.kapm.onboarding_clientes.dto.request.ClienteUpdateRequest;
import com.kapm.onboarding_clientes.dto.request.DatosLaboralesDTO;
import com.kapm.onboarding_clientes.dto.request.DomicilioDTO;
import com.kapm.onboarding_clientes.dto.request.LoginRequest;
import com.kapm.onboarding_clientes.dto.response.ClienteResponse;
import com.kapm.onboarding_clientes.dto.response.CuentaResponse;
import com.kapm.onboarding_clientes.dto.response.LoginResponse;
import com.kapm.onboarding_clientes.exception.*;
import com.kapm.onboarding_clientes.model.*;
import com.kapm.onboarding_clientes.model.catalogo.*;
import com.kapm.onboarding_clientes.repository.ClienteRepository;
import com.kapm.onboarding_clientes.repository.DatosSeguridadBiometriaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Period;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ClienteServiceImpl implements ClienteService {

    private final ClienteRepository clienteRepository;
    private final CuentaService cuentaService;
    private final SeguridadBiometriaService seguridadBiometriaService;
    private final DatosSeguridadBiometriaRepository seguridadRepository;
    private final CatalogoService catalogoService;

    @Override
    @Transactional
    public ClienteResponse registrarCliente(ClienteRegistrationRequest request) {
        
        validarMayoriaDeEdad(request.getFechaNacimiento());

        
        String curp = request.getCurp().trim().toUpperCase();
        if (clienteRepository.existsByCurp(curp)) {
            throw new CurpDuplicadaException(curp);
        }

        String rfc = request.getRfc().trim().toUpperCase();
        if (clienteRepository.existsByRfc(rfc)) {
            throw new RfcDuplicadoException(rfc);
        }

        String correo = request.getCorreo().trim().toLowerCase();
        if (clienteRepository.existsByCorreo(correo)) {
            throw new CorreoDuplicadoException(correo);
        }

        // Búsqueda y asociación de entidades de catálogo (Foreign Keys)
        CatSexo sexoCat = catalogoService.buscarSexo(request.getSexo());
        CatEstadoCivil estadoCivilCat = catalogoService.buscarEstadoCivil(request.getEstadoCivil());
        CatNacionalidad nacionalidadCat = catalogoService.buscarNacionalidad(request.getNacionalidad());
        CatOcupacion ocupacionCat = catalogoService.buscarOcupacion(request.getLaboral().getOcupacion());
        CatEstadoRepublica estadoDomicilioCat = catalogoService.buscarEstadoRepublica(request.getDomicilio().getEstado());
        CatPais paisDomicilioCat = catalogoService.buscarPais(request.getDomicilio().getPais());

        // 3. Crear Domicilio
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

        // 4. Crear Cuenta Bancaria Asociada Automáticamente y Registro de Saldo
        Cuenta cuenta = cuentaService.crearCuentaInicial(request.getSaldoInicial());

        // 5. Crear Datos de Seguridad y Biometría
        DatosSeguridadBiometria seguridad = crearOPrepararSeguridad(request, curp);

        // 6. Construir Entidad Cliente
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

        return mapearAClienteResponse(clienteGuardado);
    }

    @Override
    @Transactional
    public List<ClienteResponse> obtenerTodosLosClientes() {
        return clienteRepository.findAll().stream()
                .map(this::mapearAClienteResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public List<ClienteResponse> obtenerClientesActivos() {
        return clienteRepository.findByActivoTrue().stream()
                .map(this::mapearAClienteResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ClienteResponse obtenerClientePorId(Long id) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new ClienteNoEncontradoException(id));
        return mapearAClienteResponse(cliente);
    }

    @Override
    @Transactional
    public ClienteResponse obtenerClientePorCurp(String curp) {
        Cliente cliente = clienteRepository.findByCurp(curp.trim().toUpperCase())
                .orElseThrow(() -> new ClienteNoEncontradoException("No se encontró cliente con CURP: " + curp));
        return mapearAClienteResponse(cliente);
    }

    @Override
    @Transactional
    public ClienteResponse obtenerClientePorRfc(String rfc) {
        Cliente cliente = clienteRepository.findByRfc(rfc.trim().toUpperCase())
                .orElseThrow(() -> new ClienteNoEncontradoException("No se encontró cliente con RFC: " + rfc));
        return mapearAClienteResponse(cliente);
    }

    @Override
    @Transactional
    public ClienteResponse obtenerClientePorCorreo(String correo) {
        Cliente cliente = clienteRepository.findByCorreo(correo.trim().toLowerCase())
                .orElseThrow(() -> new ClienteNoEncontradoException("No se encontró cliente con correo: " + correo));
        return mapearAClienteResponse(cliente);
    }

    @Override
    @Transactional
    public ClienteResponse obtenerClientePorNumeroCuenta(String numeroCuenta) {
        Cliente cliente = clienteRepository.findByCuentaNumeroCuenta(numeroCuenta)
                .orElseThrow(() -> new ClienteNoEncontradoException("No se encontró cliente asociado al número de cuenta: " + numeroCuenta));
        return mapearAClienteResponse(cliente);
    }

    @Override
    @Transactional
    public List<ClienteResponse> obtenerClientesPorRangoFechas(LocalDate fechaInicio, LocalDate fechaFin) {
        if (fechaInicio == null || fechaFin == null) {
            throw new ReglaNegocioException("Las fechas de inicio y fin son obligatorias para la consulta por rango");
        }
        if (fechaInicio.isAfter(fechaFin)) {
            throw new ReglaNegocioException("La fecha de inicio no puede ser posterior a la fecha de fin");
        }

        LocalDateTime inicio = fechaInicio.atStartOfDay();
        LocalDateTime fin = fechaFin.atTime(LocalTime.MAX);

        return clienteRepository.findByFechaRegistroBetween(inicio, fin).stream()
                .map(this::mapearAClienteResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ClienteResponse actualizarCliente(Long id, ClienteUpdateRequest request) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new ClienteNoEncontradoException(id));

        if (!cliente.getActivo()) {
            throw new ReglaNegocioException("No se puede actualizar la información de un cliente dado de baja (inactivo)");
        }

        validarMayoriaDeEdad(request.getFechaNacimiento());

        String nuevoCorreo = request.getCorreo().trim().toLowerCase();
        if (!nuevoCorreo.equalsIgnoreCase(cliente.getCorreo()) && clienteRepository.existsByCorreo(nuevoCorreo)) {
            throw new CorreoDuplicadoException(nuevoCorreo);
        }

        // Búsqueda y asociación de entidades de catálogo (Foreign Keys)
        CatSexo sexoCat = catalogoService.buscarSexo(request.getSexo());
        CatEstadoCivil estadoCivilCat = catalogoService.buscarEstadoCivil(request.getEstadoCivil());
        CatNacionalidad nacionalidadCat = catalogoService.buscarNacionalidad(request.getNacionalidad());
        CatOcupacion ocupacionCat = catalogoService.buscarOcupacion(request.getLaboral().getOcupacion());
        CatEstadoRepublica estadoDomicilioCat = catalogoService.buscarEstadoRepublica(request.getDomicilio().getEstado());
        CatPais paisDomicilioCat = catalogoService.buscarPais(request.getDomicilio().getPais());

        cliente.setNombre(request.getNombre().trim());
        cliente.setSegundoNombre(request.getSegundoNombre() != null ? request.getSegundoNombre().trim() : null);
        cliente.setApellidoPaterno(request.getApellidoPaterno().trim());
        cliente.setApellidoMaterno(request.getApellidoMaterno().trim());
        cliente.setFechaNacimiento(request.getFechaNacimiento());
        cliente.setSexo(sexoCat);
        cliente.setNacionalidad(nacionalidadCat);
        cliente.setEstadoCivil(estadoCivilCat);

        cliente.setCorreo(nuevoCorreo);
        cliente.setTelefonoMovil(request.getTelefonoMovil().trim());
        cliente.setTelefonoAlternativo(request.getTelefonoAlternativo() != null && !request.getTelefonoAlternativo().isBlank() ? request.getTelefonoAlternativo().trim() : null);

        Domicilio dom = cliente.getDomicilio();
        dom.setCalle(request.getDomicilio().getCalle().trim());
        dom.setNumeroExterior(request.getDomicilio().getNumeroExterior().trim());
        dom.setNumeroInterior(request.getDomicilio().getNumeroInterior() != null ? request.getDomicilio().getNumeroInterior().trim() : null);
        dom.setColonia(request.getDomicilio().getColonia().trim());
        dom.setMunicipio(request.getDomicilio().getMunicipio().trim());
        dom.setEstado(estadoDomicilioCat);
        dom.setCodigoPostal(request.getDomicilio().getCodigoPostal().trim());
        dom.setPais(paisDomicilioCat);

        cliente.setOcupacion(ocupacionCat);
        cliente.setEmpresa(request.getLaboral().getEmpresa().trim());
        cliente.setIngresoMensual(request.getLaboral().getIngresoMensual());

        Cliente clienteActualizado = clienteRepository.save(cliente);
        return mapearAClienteResponse(clienteActualizado);
    }

    @Override
    @Transactional
    public void desactivarCliente(Long id) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new ClienteNoEncontradoException(id));

        if (!cliente.getActivo()) {
            throw new ReglaNegocioException("El cliente ya se encuentra inactivo");
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
    public LoginResponse autenticarCliente(LoginRequest request) {
        DatosSeguridadBiometria seguridad = seguridadRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado: " + request.getUsername()));

        Cliente cliente = clienteRepository.findAll().stream()
                .filter(c -> c.getDatosSeguridad() != null && c.getDatosSeguridad().getId().equals(seguridad.getId()))
                .findFirst()
                .orElse(null);

        if (cliente != null && !cliente.getActivo()) {
            seguridad.setLoggeado(false);
            seguridadRepository.save(seguridad);
            throw new ReglaNegocioException("El cliente asociado a este usuario está inactivo");
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
            return LoginResponse.builder()
                    .autenticado(false)
                    .loggeado(false)
                    .mensaje("Autenticación fallida. Credenciales o datos biométricos incorrectos.")
                    .username(request.getUsername())
                    .estadoSesion("DENEGADA")
                    .timestamp(LocalDateTime.now())
                    .build();
        }

     
        seguridad.setLoggeado(true);
        seguridad.setUltimaActividad(LocalDateTime.now());
        seguridadRepository.save(seguridad);

        String nombreCompleto = cliente != null ? cliente.getNombre() + " " + cliente.getApellidoPaterno() : "Usuario Sistema";
        String numeroCuenta = cliente != null && cliente.getCuenta() != null ? cliente.getCuenta().getNumeroCuenta() : "N/A";
        Long clienteId = cliente != null ? cliente.getId() : null;

        return LoginResponse.builder()
                .autenticado(true)
                .loggeado(true)
                .mensaje("Autenticación exitosa mediante " + metodoAuth)
                .username(seguridad.getUsername())
                .clienteId(clienteId)
                .clienteNombreCompleto(nombreCompleto)
                .numeroCuenta(numeroCuenta)
                .tipoBiometriaValidada(seguridad.getTipoBiometria())
                .tokenSimulado("SEC-TOKEN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .segundosDesdeUltimaActividad(0L)
                .estadoSesion("ACTIVA (Límite Inactividad: 5 Segundos)")
                .timestamp(LocalDateTime.now())
                .build();
    }

    private void validarMayoriaDeEdad(LocalDate fechaNacimiento) {
        if (fechaNacimiento == null) {
            throw new ReglaNegocioException("La fecha de nacimiento es obligatoria");
        }
        if (fechaNacimiento.isAfter(LocalDate.now())) {
            throw new ReglaNegocioException("La fecha de nacimiento no puede ser una fecha futura");
        }
        int edad = Period.between(fechaNacimiento, LocalDate.now()).getYears();
        if (edad < 18) {
            throw new ReglaNegocioException("El cliente debe ser mayor de edad (18 años o más). Edad calculada: " + edad + " años.");
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

    private ClienteResponse mapearAClienteResponse(Cliente cliente) {
        int edad = Period.between(cliente.getFechaNacimiento(), LocalDate.now()).getYears();

        String nombreCompleto = cliente.getNombre() +
                (cliente.getSegundoNombre() != null && !cliente.getSegundoNombre().isBlank() ? " " + cliente.getSegundoNombre() : "") +
                " " + cliente.getApellidoPaterno() + " " + cliente.getApellidoMaterno();

        Domicilio dom = cliente.getDomicilio();
        DomicilioDTO domDTO = DomicilioDTO.builder()
                .calle(dom.getCalle())
                .numeroExterior(dom.getNumeroExterior())
                .numeroInterior(dom.getNumeroInterior())
                .colonia(dom.getColonia())
                .municipio(dom.getMunicipio())
                .estado(dom.getEstado() != null ? dom.getEstado().getDescripcion() : null)
                .codigoPostal(dom.getCodigoPostal())
                .pais(dom.getPais() != null ? dom.getPais().getDescripcion() : null)
                .build();

        DatosLaboralesDTO labDTO = DatosLaboralesDTO.builder()
                .ocupacion(cliente.getOcupacion() != null ? cliente.getOcupacion().getDescripcion() : null)
                .empresa(cliente.getEmpresa())
                .ingresoMensual(cliente.getIngresoMensual() != null
                        ? cliente.getIngresoMensual().setScale(2, RoundingMode.HALF_UP)
                        : null)
                .build();

        CuentaResponse cuentaResponse = null;
        if (cliente.getCuenta() != null) {
            cuentaResponse = CuentaResponse.builder()
                    .id(cliente.getCuenta().getId())
                    .numeroCuenta(cliente.getCuenta().getNumeroCuenta())
                    .saldo(cliente.getCuenta().getSaldo() != null
                            ? cliente.getCuenta().getSaldo().setScale(2, RoundingMode.HALF_UP)
                            : null)
                    .estatus(cliente.getCuenta().getEstatus())
                    .fechaCreacion(cliente.getCuenta().getFechaCreacion())
                    .clienteNombreCompleto(nombreCompleto)
                    .clienteCurp(cliente.getCurp())
                    .build();
        }

        String username = null;
        TipoBiometria tipoBio = null;
        String hashBioResumido = null;
        Boolean loggeado = false;
        Long segundosInactividad = null;

        if (cliente.getDatosSeguridad() != null) {
            var seguridad = cliente.getDatosSeguridad();
            username = seguridad.getUsername();
            tipoBio = seguridad.getTipoBiometria();
            String fullHash = seguridad.getHashBiometrico();
            hashBioResumido = fullHash != null && fullHash.length() > 12 ? fullHash.substring(0, 12) + "..." : fullHash;

            if (seguridad.getUltimaActividad() != null) {
                segundosInactividad = Duration.between(seguridad.getUltimaActividad(), LocalDateTime.now()).getSeconds();
                
                if (Boolean.TRUE.equals(seguridad.getLoggeado()) && segundosInactividad <= 5) {
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
                .id(cliente.getId())
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
                .hashBiometricoResumido(hashBioResumido)
                .segundosUltimaActividad(segundosInactividad)
                .activo(cliente.getActivo())
                .fechaRegistro(cliente.getFechaRegistro())
                .build();
    }
}
