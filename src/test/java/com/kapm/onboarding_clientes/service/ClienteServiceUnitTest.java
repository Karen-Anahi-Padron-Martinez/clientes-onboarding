package com.kapm.onboarding_clientes.service;

import com.kapm.onboarding_clientes.dto.request.*;
import com.kapm.onboarding_clientes.dto.response.ClienteResponse;
import com.kapm.onboarding_clientes.dto.response.CuentaResponse;
import com.kapm.onboarding_clientes.dto.response.LoginResponse;
import com.kapm.onboarding_clientes.dto.response.ValidacionIdentificadorResponse;
import com.kapm.onboarding_clientes.entity.*;
import com.kapm.onboarding_clientes.exception.*;
import com.kapm.onboarding_clientes.mapper.ClienteMapper;
import com.kapm.onboarding_clientes.repository.ClienteRepository;
import com.kapm.onboarding_clientes.repository.DatosSeguridadBiometriaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ClienteServiceImpl (unitarias): reglas de registro, actualización, ciclo de vida y login")
class ClienteServiceUnitTest {

    private static final String CURP = "PEGJ900515HDFRMN01";
    private static final String RFC = "PEGJ9005151A2";

    @Mock ClienteRepository clienteRepository;
    @Mock CuentaService cuentaService;
    @Mock SeguridadBiometriaService seguridadService;
    @Mock DatosSeguridadBiometriaRepository seguridadRepository;
    @Mock CatalogoService catalogoService;
    @Mock ClienteMapper clienteMapper;
    @Mock TokenService tokenService;

    private ClienteServiceImpl servicio;
    private final ClienteResponse respuesta = new ClienteResponse();

    @BeforeEach
    void setUp() {
        servicio = new ClienteServiceImpl(clienteRepository, cuentaService, seguridadService, seguridadRepository,
                catalogoService, clienteMapper, tokenService, 0);
        when(clienteRepository.save(any(Cliente.class))).then(returnsFirstArg());
        when(clienteMapper.mapearAClienteResponse(any())).thenReturn(respuesta);
    }

    private static Cliente cliente(Boolean activo) {
        return Cliente.builder().id(1L).curp(CURP).rfc(RFC).nombre("Juan").apellidoPaterno("Pérez")
                .correo("actual@correo.com").activo(activo).build();
    }

    private void existePorCurp(Cliente cliente) {
        when(clienteRepository.findByCurp(CURP)).thenReturn(Optional.ofNullable(cliente));
    }

    private void existePorRfc(Cliente cliente) {
        when(clienteRepository.findByRfc(RFC)).thenReturn(Optional.ofNullable(cliente));
    }

    // ------------------------------------------------------------------ registro

    private static ClienteRegistrationRequest registro(Consumer<ClienteRegistrationRequest> cambio) {
        ClienteRegistrationRequest r = ClienteRegistrationRequest.builder()
                .nombre(" Juan ").apellidoPaterno("Pérez").apellidoMaterno("Gómez")
                .fechaNacimiento(LocalDate.of(1990, 5, 15)).curp(CURP.toLowerCase()).rfc(RFC).correo(" Juan@Correo.com ")
                .telefonoMovil("5512345678").sexo("MASCULINO").estadoCivil("SOLTERO").nacionalidad("Mexicana")
                .domicilio(DomicilioDTO.builder().calle("C").numeroExterior("1").colonia("Col").municipio("M")
                        .estado("Jalisco").codigoPostal("44100").pais("México").build())
                .laboral(DatosLaboralesDTO.builder().ocupacion("Estudiante").empresa("E").build())
                .build();
        cambio.accept(r);
        return r;
    }

    @Test
    @DisplayName("Registro: fecha de nacimiento obligatoria, no futura y mayor de edad")
    void registroEdad() {
        assertThatThrownBy(() -> servicio.registrarCliente(registro(r -> r.setFechaNacimiento(null))))
                .hasMessage(ErrorMessage.FECHA_NACIMIENTO_REQUERIDA.getMensaje());
        assertThatThrownBy(() -> servicio.registrarCliente(registro(r -> r.setFechaNacimiento(LocalDate.now().plusDays(1)))))
                .hasMessage(ErrorMessage.FECHA_NACIMIENTO_FUTURA.getMensaje());
        assertThatThrownBy(() -> servicio.registrarCliente(registro(r -> r.setFechaNacimiento(LocalDate.now().minusYears(17)))))
                .hasMessageStartingWith(ErrorMessage.CLIENTE_MENOR_DE_EDAD.getMensaje());
    }

    @Test
    @DisplayName("Registro: formato y unicidad de CURP, RFC y correo")
    void registroUnicidad() {
        assertThatThrownBy(() -> servicio.registrarCliente(registro(r -> r.setCurp("MALA")))).isInstanceOf(ReglaNegocioException.class);
        when(clienteRepository.existsByCurp(CURP)).thenReturn(true);
        assertThatThrownBy(() -> servicio.registrarCliente(registro(r -> { }))).isInstanceOf(CurpDuplicadaException.class);
        when(clienteRepository.existsByCurp(CURP)).thenReturn(false);

        assertThatThrownBy(() -> servicio.registrarCliente(registro(r -> r.setRfc("MALO")))).isInstanceOf(ReglaNegocioException.class);
        when(clienteRepository.existsByRfc(RFC)).thenReturn(true);
        assertThatThrownBy(() -> servicio.registrarCliente(registro(r -> { }))).isInstanceOf(RfcDuplicadoException.class);
        when(clienteRepository.existsByRfc(RFC)).thenReturn(false);

        when(clienteRepository.existsByCorreo("juan@correo.com")).thenReturn(true);
        assertThatThrownBy(() -> servicio.registrarCliente(registro(r -> { }))).isInstanceOf(CorreoDuplicadoException.class);
    }

    @Test
    @DisplayName("Registro: sin credenciales genera usuario por defecto; los opcionales vacíos quedan nulos")
    void registroSinSeguridad() {
        assertThat(servicio.registrarCliente(registro(r -> r.setTelefonoAlternativo(" ")))).isSameAs(respuesta);
        verify(seguridadService).crearDatosSeguridad(eq("usr_pegj900515"), anyString(), eq(TipoBiometria.HUELLA_DACTILAR), eq("BIO-TEMPLATE-" + CURP));

        servicio.registrarCliente(registro(r -> r.setSeguridad(DatosSeguridadDTO.builder().username(null).build())));
        servicio.registrarCliente(registro(r -> r.setSeguridad(DatosSeguridadDTO.builder().username(" ").build())));
        verify(seguridadService, times(3)).crearDatosSeguridad(eq("usr_pegj900515"), anyString(), any(), anyString());

        ArgumentCaptor<Cliente> guardados = ArgumentCaptor.forClass(Cliente.class);
        verify(clienteRepository, times(3)).save(guardados.capture());
        Cliente guardado = guardados.getAllValues().get(0);
        assertThat(guardado.getNombre()).isEqualTo("Juan");
        assertThat(guardado.getCorreo()).isEqualTo("juan@correo.com");
        assertThat(guardado.getSegundoNombre()).isNull();
        assertThat(guardado.getTelefonoAlternativo()).isNull();
        assertThat(guardado.getDomicilio().getNumeroInterior()).isNull();
    }

    @Test
    @DisplayName("Registro: con credenciales propias y datos opcionales informados")
    void registroConSeguridad() {
        servicio.registrarCliente(registro(r -> {
            r.setSegundoNombre(" Carlos ");
            r.setTelefonoAlternativo(" 5587654321 ");
            r.getDomicilio().setNumeroInterior(" A ");
            r.setSeguridad(DatosSeguridadDTO.builder().username(" juanp ").password("p").tipoBiometria(TipoBiometria.IRIS).datosBiometricos("b").build());
        }));
        verify(seguridadService).crearDatosSeguridad("juanp", "p", TipoBiometria.IRIS, "b");
        ArgumentCaptor<Cliente> guardado = ArgumentCaptor.forClass(Cliente.class);
        verify(clienteRepository).save(guardado.capture());
        assertThat(guardado.getValue().getSegundoNombre()).isEqualTo("Carlos");
        assertThat(guardado.getValue().getTelefonoAlternativo()).isEqualTo("5587654321");
        assertThat(guardado.getValue().getDomicilio().getNumeroInterior()).isEqualTo("A");
    }

    // ------------------------------------------------------------------ consultas

    @Test
    @DisplayName("Consultas: listados, búsqueda paginada y búsquedas por correo y cuenta")
    @SuppressWarnings("unchecked")
    void consultas() {
        when(clienteRepository.findAll()).thenReturn(List.of(cliente(true)));
        when(clienteRepository.findByActivoTrue()).thenReturn(List.of(cliente(true)));
        assertThat(servicio.obtenerTodosLosClientes()).containsExactly(respuesta);
        assertThat(servicio.obtenerClientesActivos()).containsExactly(respuesta);

        BuscarClienteRequest buscar = BuscarClienteRequest.builder().nombre("Juan").build();
        when(clienteRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(Page.empty());
        assertThatThrownBy(() -> servicio.buscarClientes(buscar)).isInstanceOf(ClienteNoEncontradoException.class);
        Page<Cliente> pagina = new PageImpl<>(List.of(cliente(true)));
        when(clienteRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(pagina);
        assertThat(servicio.buscarClientes(buscar).getContenido()).containsExactly(respuesta);

        when(clienteRepository.findByCorreo("a@b.com")).thenReturn(Optional.of(cliente(true)));
        assertThat(servicio.obtenerClientePorCorreo(" A@B.com ")).isSameAs(respuesta);
        assertThatThrownBy(() -> servicio.obtenerClientePorCorreo("x@y.com")).isInstanceOf(ClienteNoEncontradoException.class);
        when(clienteRepository.findByCuentaNumeroCuenta("1")).thenReturn(Optional.of(cliente(true)));
        assertThat(servicio.obtenerClientePorNumeroCuenta("1")).isSameAs(respuesta);
        assertThatThrownBy(() -> servicio.obtenerClientePorNumeroCuenta("2")).isInstanceOf(ClienteNoEncontradoException.class);
    }

    @Test
    @DisplayName("Búsquedas por CURP/RFC validan formato y existencia")
    void busquedasPorIdentificador() {
        assertThatThrownBy(() -> servicio.obtenerClientePorCurp("MALA")).isInstanceOf(ReglaNegocioException.class);
        assertThatThrownBy(() -> servicio.obtenerClientePorRfc("MALO")).isInstanceOf(ReglaNegocioException.class);
        assertThatThrownBy(() -> servicio.obtenerClientePorCurp(CURP)).isInstanceOf(ClienteNoEncontradoException.class);
        assertThatThrownBy(() -> servicio.obtenerClientePorRfc(RFC)).isInstanceOf(ClienteNoEncontradoException.class);
        existePorRfc(cliente(true));
        assertThat(servicio.obtenerClientePorRfc(RFC.toLowerCase())).isSameAs(respuesta);
    }

    // ------------------------------------------------------------------ actualización

    private static ClienteUpdateRequest update(Consumer<ClienteUpdateRequest> cambio) {
        ClienteUpdateRequest r = ClienteUpdateRequest.builder().correo(" Actual@Correo.com ").telefonoMovil(" 5511111111 ").build();
        cambio.accept(r);
        return r;
    }

    @Test
    @DisplayName("Actualizar: requiere identificador y cliente activo")
    void actualizarPrecondiciones() {
        assertThatThrownBy(() -> servicio.actualizarCliente(update(r -> { }))).hasMessage(ErrorMessage.IDENTIFICADOR_REQUERIDO.getMensaje());
        assertThatThrownBy(() -> servicio.actualizarCliente(update(r -> { r.setCurp(" "); r.setRfc(" "); })))
                .hasMessage(ErrorMessage.IDENTIFICADOR_REQUERIDO.getMensaje());
        existePorCurp(cliente(false));
        assertThatThrownBy(() -> servicio.actualizarCliente(update(r -> r.setCurp(CURP)))).hasMessage(ErrorMessage.CLIENTE_INACTIVO.getMensaje());
        assertThatThrownBy(() -> servicio.actualizarClientePorRfc("MALO", update(r -> { }))).isInstanceOf(ReglaNegocioException.class);
    }

    @Test
    @DisplayName("Actualizar: mismo correo no consulta unicidad; correo ajeno se rechaza")
    void actualizarCorreo() {
        Cliente actual = cliente(true);
        existePorRfc(actual);
        servicio.actualizarCliente(update(r -> r.setRfc(RFC)));
        verify(clienteRepository, never()).existsByCorreoAndIdNot(anyString(), any());
        assertThat(actual.getSexo()).isNull();
        assertThat(actual.getTelefonoMovil()).isEqualTo("5511111111");

        existePorCurp(actual);
        assertThat(servicio.actualizarCliente(update(r -> r.setCurp(CURP)))).isSameAs(respuesta);

        when(clienteRepository.existsByCorreoAndIdNot("otro@correo.com", 1L)).thenReturn(true);
        assertThatThrownBy(() -> servicio.actualizarCliente(update(r -> { r.setRfc(RFC); r.setCorreo("otro@correo.com"); })))
                .isInstanceOf(CorreoDuplicadoException.class);
    }

    @Test
    @DisplayName("Actualizar: catálogos opcionales, domicilio nuevo o existente y datos laborales")
    void actualizarCamposModificables() {
        Cliente actual = cliente(true);
        existePorCurp(actual);
        DomicilioDTO domicilio = DomicilioDTO.builder().calle(" C ").numeroExterior("1").colonia("Col").municipio("M")
                .estado("Jalisco").codigoPostal("44100").pais("México").build();
        DatosLaboralesDTO laboral = DatosLaboralesDTO.builder().ocupacion("Estudiante").empresa(" E ").ingresoMensual(BigDecimal.TEN).build();

        servicio.actualizarClientePorCurp(ActualizarPorCurpRequest.builder().curp(CURP).sexo(" ").estadoCivil(" ")
                .correo("nuevo@correo.com").telefonoMovil("5522222222").telefonoAlternativo(" 5533333333 ")
                .domicilio(domicilio).laboral(laboral).build());
        assertThat(actual.getCorreo()).isEqualTo("nuevo@correo.com");
        assertThat(actual.getTelefonoAlternativo()).isEqualTo("5533333333");
        assertThat(actual.getDomicilio().getCalle()).isEqualTo("C");
        assertThat(actual.getEmpresa()).isEqualTo("E");
        verify(catalogoService, never()).buscarSexo(any());

        Domicilio existente = actual.getDomicilio();
        domicilio.setNumeroInterior(" B ");
        existePorRfc(actual);
        servicio.actualizarClientePorRfc(ActualizarPorRfcRequest.builder().rfc(RFC).sexo("FEMENINO").estadoCivil("CASADO")
                .correo("nuevo@correo.com").telefonoMovil("5522222222").domicilio(domicilio).build());
        verify(catalogoService).buscarSexo("FEMENINO");
        verify(catalogoService).buscarEstadoCivil("CASADO");
        assertThat(actual.getDomicilio()).isSameAs(existente);
        assertThat(existente.getNumeroInterior()).isEqualTo("B");
    }

    // ------------------------------------------------------------------ baja y reactivación

    @Test
    @DisplayName("Baja lógica: por CURP o RFC, con o sin cuenta y seguridad, y no dos veces")
    void bajaLogica() {
        Cliente completo = cliente(true);
        completo.setCuenta(Cuenta.builder().estatus(EstatusCuenta.ACTIVA).build());
        completo.setDatosSeguridad(DatosSeguridadBiometria.builder().loggeado(true).build());
        existePorCurp(completo);
        servicio.desactivarCliente(new ClienteIdentificadorRequest(CURP, null));
        assertThat(completo.getActivo()).isFalse();
        assertThat(completo.getCuenta().getEstatus()).isEqualTo(EstatusCuenta.INACTIVA);
        assertThat(completo.getDatosSeguridad().getLoggeado()).isFalse();
        assertThatThrownBy(() -> servicio.desactivarClientePorCurp(CURP)).hasMessage(ErrorMessage.CLIENTE_YA_INACTIVO.getMensaje());

        Cliente minimo = cliente(true);
        existePorRfc(minimo);
        servicio.desactivarCliente(new ClienteIdentificadorRequest(null, RFC));
        assertThat(minimo.getActivo()).isFalse();
        minimo.setActivo(true);
        servicio.desactivarClientePorRfc(RFC);
        assertThat(minimo.getActivo()).isFalse();
        assertThatThrownBy(() -> servicio.desactivarClientePorRfc(RFC)).isInstanceOf(ReglaNegocioException.class);
        assertThatThrownBy(() -> servicio.desactivarCliente(new ClienteIdentificadorRequest())).isInstanceOf(ReglaNegocioException.class);
    }

    @Test
    @DisplayName("Reactivación: por CURP o RFC, con o sin cuenta; un cliente activo no se reactiva")
    void reactivacion() {
        Cliente conCuenta = cliente(false);
        conCuenta.setCuenta(Cuenta.builder().estatus(EstatusCuenta.INACTIVA).build());
        existePorCurp(conCuenta);
        assertThat(servicio.reactivarCliente(new ClienteIdentificadorRequest(CURP, null))).isSameAs(respuesta);
        assertThat(conCuenta.getCuenta().getEstatus()).isEqualTo(EstatusCuenta.ACTIVA);
        assertThatThrownBy(() -> servicio.reactivarClientePorCurp(CURP)).hasMessageEndingWith(": " + CURP);

        Cliente sinCuenta = cliente(false);
        existePorRfc(sinCuenta);
        servicio.reactivarCliente(new ClienteIdentificadorRequest(null, RFC.toLowerCase()));
        assertThat(sinCuenta.getActivo()).isTrue();
        assertThatThrownBy(() -> servicio.reactivarClientePorRfc(RFC)).hasMessageEndingWith(": " + RFC);
        assertThatThrownBy(() -> servicio.reactivarCliente(new ClienteIdentificadorRequest(" ", " "))).isInstanceOf(ReglaNegocioException.class);
    }

    // ------------------------------------------------------------------ disponibilidad

    @Test
    @DisplayName("Disponibilidad de CURP y RFC: vacío, formato inválido, registrado y disponible")
    void disponibilidad() {
        assertThat(servicio.validarDisponibilidadCurp(null).isFormatoValido()).isFalse();
        assertThat(servicio.validarDisponibilidadCurp(" ").getIdentificador()).isEqualTo(" ");
        assertThat(servicio.validarDisponibilidadCurp("mala").getIdentificador()).isEqualTo("MALA");
        when(clienteRepository.existsByCurp(CURP)).thenReturn(true);
        assertThat(servicio.validarDisponibilidad(new ValidarIdentificadorRequest(CURP, null)).isDisponible()).isFalse();
        when(clienteRepository.existsByCurp(CURP)).thenReturn(false);
        assertThat(servicio.validarDisponibilidadCurp(CURP).getMensaje()).contains("disponible");

        assertThat(servicio.validarDisponibilidadRfc(null).getTipo()).isEqualTo("RFC");
        assertThat(servicio.validarDisponibilidadRfc(" ").isFormatoValido()).isFalse();
        assertThat(servicio.validarDisponibilidadRfc("malo").getIdentificador()).isEqualTo("MALO");
        when(clienteRepository.existsByRfc(RFC)).thenReturn(true);
        ValidacionIdentificadorResponse registrado = servicio.validarDisponibilidad(new ValidarIdentificadorRequest(null, RFC));
        assertThat(registrado.getMensaje()).isEqualTo(ErrorMessage.RFC_DUPLICADO.getMensaje());
        when(clienteRepository.existsByRfc(RFC)).thenReturn(false);
        assertThat(servicio.validarDisponibilidadRfc(RFC).isDisponible()).isTrue();
        assertThatThrownBy(() -> servicio.validarDisponibilidad(new ValidarIdentificadorRequest())).isInstanceOf(ReglaNegocioException.class);
    }

    // ------------------------------------------------------------------ contacto y cuenta

    @Test
    @DisplayName("Contacto: por CURP o RFC, cliente activo y correo único")
    void contacto() {
        Cliente actual = cliente(true);
        existePorRfc(actual);
        ActualizarContactoRequest porRfc = ActualizarContactoRequest.builder().rfc(RFC).correo("nuevo@correo.com")
                .telefonoMovil(" 5544444444 ").telefonoAlternativo(null).build();
        assertThat(servicio.actualizarContacto(porRfc)).isSameAs(respuesta);
        assertThat(actual.getTelefonoMovil()).isEqualTo("5544444444");

        existePorCurp(actual);
        ActualizarContactoRequest activoPorCurp = ActualizarContactoRequest.builder().curp(CURP).correo("nuevo@correo.com")
                .telefonoMovil("5544444444").telefonoAlternativo(" 5555555555 ").build();
        assertThat(servicio.actualizarContacto(activoPorCurp)).isSameAs(respuesta);
        assertThat(actual.getTelefonoAlternativo()).isEqualTo("5555555555");

        existePorCurp(cliente(false));
        ActualizarContactoRequest porCurp = ActualizarContactoRequest.builder().curp(CURP).correo("x@correo.com").telefonoMovil("1").build();
        assertThatThrownBy(() -> servicio.actualizarContacto(porCurp)).hasMessage(ErrorMessage.CLIENTE_INACTIVO.getMensaje());
        assertThatThrownBy(() -> servicio.actualizarContacto(new ActualizarContactoRequest())).isInstanceOf(ReglaNegocioException.class);
    }

    @Test
    @DisplayName("Cuenta: por CURP o RFC; un cliente sin cuenta responde 404")
    void cuenta() {
        Cliente conCuenta = cliente(true);
        conCuenta.setCuenta(Cuenta.builder().numeroCuenta("1234567890").build());
        CuentaResponse cuenta = new CuentaResponse();
        when(cuentaService.obtenerCuentaPorNumero("1234567890")).thenReturn(cuenta);
        existePorCurp(conCuenta);
        assertThat(servicio.obtenerCuenta(new ClienteIdentificadorRequest(CURP, null))).isSameAs(cuenta);

        existePorRfc(conCuenta);
        assertThat(servicio.obtenerCuentaPorRfc(RFC)).isSameAs(cuenta);
        existePorRfc(cliente(true));
        assertThatThrownBy(() -> servicio.obtenerCuenta(new ClienteIdentificadorRequest(null, RFC)))
                .hasMessage(ErrorMessage.CLIENTE_SIN_CUENTA.getMensaje());
        assertThatThrownBy(() -> servicio.obtenerCuenta(new ClienteIdentificadorRequest())).isInstanceOf(ReglaNegocioException.class);
    }

    @Test
    @DisplayName("El titular del token se resuelve por username; uno desconocido no tiene acceso")
    void curpPorUsername() {
        when(clienteRepository.findByDatosSeguridadUsername("juan")).thenReturn(Optional.of(cliente(true)));
        assertThat(servicio.obtenerCurpPorUsername("juan")).isEqualTo(CURP);
        assertThatThrownBy(() -> servicio.obtenerCurpPorUsername("nadie")).isInstanceOf(AccesoDenegadoException.class);
    }

    // ------------------------------------------------------------------ login

    private DatosSeguridadBiometria credenciales() {
        DatosSeguridadBiometria seguridad = DatosSeguridadBiometria.builder().username("juan").passwordHash("H")
                .hashBiometrico("B").tipoBiometria(TipoBiometria.IRIS).build();
        when(seguridadRepository.findByUsername("juan")).thenReturn(Optional.of(seguridad));
        when(tokenService.emitir(eq("juan"), anyString())).thenReturn(new TokenService.TokenEmitido("jwt", Instant.now()));
        return seguridad;
    }

    @Test
    @DisplayName("Login: usuario inexistente, cliente inactivo y credenciales incorrectas")
    void loginRechazado() {
        LoginRequest sinCredenciales = LoginRequest.builder().username("juan").build();
        assertThatThrownBy(() -> servicio.autenticarCliente(sinCredenciales)).hasMessage(ErrorMessage.AUTH_CREDENCIALES_INVALIDAS.getMensaje());

        DatosSeguridadBiometria seguridad = credenciales();
        when(clienteRepository.findByDatosSeguridadUsername("juan")).thenReturn(Optional.of(cliente(false)));
        assertThatThrownBy(() -> servicio.autenticarCliente(sinCredenciales)).hasMessage(ErrorMessage.AUTH_CLIENTE_INACTIVO.getMensaje());

        when(clienteRepository.findByDatosSeguridadUsername("juan")).thenReturn(Optional.of(cliente(true)));
        assertThatThrownBy(() -> servicio.autenticarCliente(sinCredenciales)).hasMessage(ErrorMessage.AUTH_CREDENCIALES_INVALIDAS.getMensaje());
        LoginRequest blancos = LoginRequest.builder().username("juan").password(" ").datosBiometricos(" ").build();
        assertThatThrownBy(() -> servicio.autenticarCliente(blancos)).isInstanceOf(ReglaNegocioException.class);
        LoginRequest erroneos = LoginRequest.builder().username("juan").password("mal").datosBiometricos("mal").build();
        assertThatThrownBy(() -> servicio.autenticarCliente(erroneos)).isInstanceOf(ReglaNegocioException.class);
        assertThat(seguridad.getSesionId()).isNull();
    }

    @Test
    @DisplayName("Login por contraseña de un cliente con cuenta")
    void loginPorPassword() {
        credenciales();
        Cliente cliente = cliente(true);
        cliente.setCuenta(Cuenta.builder().numeroCuenta("1234567890").build());
        when(clienteRepository.findByDatosSeguridadUsername("juan")).thenReturn(Optional.of(cliente));
        when(seguridadService.validarPassword("ok", "H")).thenReturn(true);

        LoginResponse login = servicio.autenticarCliente(LoginRequest.builder().username("juan").password("ok").datosBiometricos("b").build());

        assertThat(login.getMensaje()).contains("Contraseña");
        assertThat(login.getNumeroCuenta()).isEqualTo("1234567890");
        assertThat(login.getClienteNombreCompleto()).isEqualTo("Juan Pérez");
        assertThat(login.getCurp()).isEqualTo(cliente.getCurp());
        assertThat(login.getRfc()).isEqualTo(cliente.getRfc());
        assertThat(login.getEstadoSesion()).contains("900 Segundos");
        verify(seguridadService, never()).validarBiometria(any(), any());
    }

    @Test
    @DisplayName("Login biométrico de un usuario sin cliente asociado, o de un cliente sin cuenta")
    void loginPorBiometria() {
        credenciales();
        when(seguridadService.validarBiometria("bio", "B")).thenReturn(true);

        LoginResponse sinCliente = servicio.autenticarCliente(LoginRequest.builder().username("juan").datosBiometricos("bio").build());
        assertThat(sinCliente.getMensaje()).contains("Biometría (IRIS)");
        assertThat(sinCliente.getClienteNombreCompleto()).isEqualTo("Usuario Sistema");
        assertThat(sinCliente.getNumeroCuenta()).isEqualTo("N/A");

        when(clienteRepository.findByDatosSeguridadUsername("juan")).thenReturn(Optional.of(cliente(true)));
        assertThat(servicio.autenticarCliente(LoginRequest.builder().username("juan").datosBiometricos("bio").build()).getNumeroCuenta())
                .isEqualTo("N/A");
    }
}
