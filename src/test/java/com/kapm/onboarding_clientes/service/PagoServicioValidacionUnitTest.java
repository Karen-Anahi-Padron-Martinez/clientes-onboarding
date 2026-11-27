package com.kapm.onboarding_clientes.service;

import com.kapm.onboarding_clientes.client.puntored.PuntoRedClient;
import com.kapm.onboarding_clientes.client.puntored.model.PuntoRedTxResponse;
import com.kapm.onboarding_clientes.client.puntored.model.SendTxPayload;
import com.kapm.onboarding_clientes.config.PuntoRedProperties;
import com.kapm.onboarding_clientes.dto.request.AplicarPagoRequest;
import com.kapm.onboarding_clientes.dto.request.EmitirCodigo2FARequest;
import com.kapm.onboarding_clientes.dto.request.SolicitarCodigoPagoRequest;
import com.kapm.onboarding_clientes.dto.response.ClienteResponse;
import com.kapm.onboarding_clientes.dto.response.CodigoVerificacionResponse;
import com.kapm.onboarding_clientes.dto.response.CuentaResponse;
import com.kapm.onboarding_clientes.dto.response.PagoResponse;
import com.kapm.onboarding_clientes.entity.EstatusCuenta;
import com.kapm.onboarding_clientes.entity.EstatusPago;
import com.kapm.onboarding_clientes.entity.PagoServicio;
import com.kapm.onboarding_clientes.entity.Saldo;
import com.kapm.onboarding_clientes.entity.catalogo.CatalogoProductoPuntoRed;
import com.kapm.onboarding_clientes.exception.*;
import com.kapm.onboarding_clientes.repository.CatalogoProductoPuntoRedRepository;
import com.kapm.onboarding_clientes.repository.PagoServicioRepository;
import com.kapm.onboarding_clientes.repository.SaldoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("PagoServicioServiceImpl (unitarias): cada validación y cada resultado del proveedor")
class PagoServicioValidacionUnitTest {

    private static final String USUARIO = "titular";
    private static final String CURP = "MEHC920824HDFRMN09";
    private static final String RFC = "MEHC9208243A8";
    private static final String CUENTA = "4819204812";
    private static final String LLAVE = "llave-1";

    @Mock PagoServicioRepository pagoServicioRepository;
    @Mock CatalogoProductoPuntoRedRepository catalogoRepository;
    @Mock PuntoRedClient puntoRedClient;
    @Mock ClienteService clienteService;
    @Mock SaldoRepository saldoRepository;
    @Mock CodigoVerificacionService codigoVerificacionService;
    @Mock PagoTransaccionalService pagoTransaccionalService;

    private PuntoRedProperties props;
    private PagoServicioServiceImpl servicio;
    private ClienteResponse cliente;
    private CatalogoProductoPuntoRed recarga;
    private CatalogoProductoPuntoRed servicioCfe;
    private Saldo saldo;

    @BeforeEach
    void setUp() {
        props = new PuntoRedProperties();
        props.setUnidad("SUCURSAL_CENTRO");
        servicio = new PagoServicioServiceImpl(pagoServicioRepository, catalogoRepository, props, puntoRedClient,
                clienteService, saldoRepository, codigoVerificacionService, pagoTransaccionalService);

        cliente = ClienteResponse.builder().curp(CURP).rfc(RFC).activo(true).telefonoMovil(" 5598765432 ")
                .cuenta(CuentaResponse.builder().numeroCuenta(CUENTA).estatus(EstatusCuenta.ACTIVA).build()).build();
        recarga = CatalogoProductoPuntoRed.builder().idProducto(55).idServicio(2).tipoFront(0).activo(true).build();
        servicioCfe = CatalogoProductoPuntoRed.builder().idProducto(120).idServicio(10).tipoFront(1).activo(true).build();
        saldo = Saldo.builder().saldoDisponible(new BigDecimal("1500.00")).build();

        when(clienteService.obtenerCurpPorUsername(USUARIO)).thenReturn(CURP);
        when(clienteService.obtenerClientePorCurp(CURP)).thenReturn(cliente);
        when(clienteService.obtenerClientePorRfc(RFC)).thenReturn(cliente);
        when(catalogoRepository.findByIdProducto(55)).thenReturn(Optional.of(recarga));
        when(catalogoRepository.findByIdProducto(120)).thenReturn(Optional.of(servicioCfe));
        when(saldoRepository.findByCuentaNumeroCuenta(CUENTA)).thenReturn(Optional.of(saldo));
        when(pagoServicioRepository.findByIdempotencyKey(anyString())).thenReturn(Optional.empty());
    }

    private static AplicarPagoRequest recargaValida() {
        return AplicarPagoRequest.builder().curp(CURP).numeroCuenta(CUENTA).idProducto(55).idServicio(2)
                .telefono("5512345678").monto(new BigDecimal("100.00")).codigo("123456").idempotencyKey(LLAVE).build();
    }

    private static AplicarPagoRequest servicioValido() {
        AplicarPagoRequest r = recargaValida();
        r.setIdProducto(120);
        r.setIdServicio(10);
        r.setTelefono(null);
        r.setReferencia(" REF-001 ");
        return r;
    }

    private void rechaza(AplicarPagoRequest request, Class<? extends Throwable> tipo) {
        assertThatThrownBy(() -> servicio.aplicarPago(request, USUARIO)).isInstanceOf(tipo);
    }

    private void rechaza(Consumer<AplicarPagoRequest> cambio, Class<? extends Throwable> tipo) {
        AplicarPagoRequest r = recargaValida();
        cambio.accept(r);
        rechaza(r, tipo);
    }

    private PagoServicio pendienteReservado() {
        PagoServicio pago = PagoServicio.builder().id(9L).upc("UPC9").curp(CURP).numeroCuenta(CUENTA).idProducto(55).idServicio(2)
                .monto(new BigDecimal("100.00")).estatus(EstatusPago.PENDIENTE).build();
        when(pagoTransaccionalService.reservarSaldoYCrearPago(eq(CUENTA), any(), any(), any(), any(), any(), any(), any(), eq(LLAVE), any(), any()))
                .thenReturn(pago);
        when(pagoTransaccionalService.registrarTimeout(eq(9L), anyString(), any())).thenReturn(pago);
        return pago;
    }

    @Test
    @DisplayName("Solicitudes nulas y sin llave de idempotencia")
    void solicitudesNulas() {
        assertThatThrownBy(() -> servicio.solicitarCodigo(null, USUARIO)).isInstanceOf(ReglaNegocioException.class);
        assertThatThrownBy(() -> servicio.aplicarPago(null, USUARIO)).isInstanceOf(ReglaNegocioException.class);
        rechaza(r -> r.setIdempotencyKey(null), ReglaNegocioException.class);
        rechaza(r -> r.setIdempotencyKey(" "), ReglaNegocioException.class);
    }

    @Test
    @DisplayName("Identificación del cliente, titularidad y estado de la cuenta")
    void identificacionYCuenta() {
        rechaza(r -> r.setNumeroCuenta(null), ReglaNegocioException.class);
        rechaza(r -> r.setNumeroCuenta(" "), ReglaNegocioException.class);
        rechaza(r -> r.setCurp("INVALIDA"), ReglaNegocioException.class);
        rechaza(r -> { r.setCurp(" "); r.setRfc("X"); }, ReglaNegocioException.class);
        rechaza(r -> { r.setCurp(null); r.setRfc(" "); }, ReglaNegocioException.class);
        rechaza(r -> r.setCurp(null), ReglaNegocioException.class);

        when(clienteService.obtenerCurpPorUsername("otro")).thenReturn("OTRA");
        assertThatThrownBy(() -> servicio.aplicarPago(recargaValida(), "otro")).isInstanceOf(AccesoDenegadoException.class);

        cliente.setActivo(null);
        rechaza(r -> { }, ReglaNegocioException.class);
        cliente.setActivo(true);

        rechaza(r -> r.setNumeroCuenta("0000000000"), ReglaNegocioException.class);
        cliente.getCuenta().setEstatus(null);
        rechaza(r -> { }, ReglaNegocioException.class);
        cliente.getCuenta().setEstatus(EstatusCuenta.BLOQUEADA);
        rechaza(r -> { }, ReglaNegocioException.class);
        cliente.setCuenta(null);
        rechaza(r -> { }, ReglaNegocioException.class);
    }

    @Test
    @DisplayName("Producto: identificadores, catálogo, servicio y tipo permitido")
    void producto() {
        rechaza(r -> r.setIdProducto(null), ReglaNegocioException.class);
        rechaza(r -> r.setIdProducto(0), ReglaNegocioException.class);
        rechaza(r -> r.setIdServicio(null), ReglaNegocioException.class);
        rechaza(r -> r.setIdServicio(0), ReglaNegocioException.class);
        rechaza(r -> r.setIdProducto(999), ReglaNegocioException.class);
        rechaza(r -> r.setIdServicio(3), ReglaNegocioException.class);
        recarga.setActivo(false);
        rechaza(r -> { }, ReglaNegocioException.class);
        recarga.setActivo(true);
        recarga.setTipoFront(null);
        rechaza(r -> { }, ReglaNegocioException.class);
        recarga.setTipoFront(7);
        rechaza(r -> { }, ReglaNegocioException.class);
    }

    @Test
    @DisplayName("Teléfono de recarga y referencia/teléfono de servicio")
    void telefonoYReferencia() {
        rechaza(r -> r.setTelefono(null), ReglaNegocioException.class);
        rechaza(r -> r.setTelefono("123"), ReglaNegocioException.class);

        AplicarPagoRequest sinReferencia = servicioValido();
        sinReferencia.setReferencia(null);
        rechaza(sinReferencia, ReglaNegocioException.class);
        AplicarPagoRequest referenciaVacia = servicioValido();
        referenciaVacia.setReferencia("   ");
        rechaza(referenciaVacia, ReglaNegocioException.class);
        AplicarPagoRequest referenciaLarga = servicioValido();
        referenciaLarga.setReferencia("R".repeat(101));
        rechaza(referenciaLarga, ReglaNegocioException.class);
        AplicarPagoRequest telefonoCorto = servicioValido();
        telefonoCorto.setTelefono("123");
        rechaza(telefonoCorto, ReglaNegocioException.class);
        AplicarPagoRequest telefonoLargo = servicioValido();
        telefonoLargo.setTelefono("1".repeat(21));
        rechaza(telefonoLargo, ReglaNegocioException.class);
    }

    @Test
    @DisplayName("Monto: obligatorio, positivo, dos decimales, dentro de rango y con saldo suficiente")
    void monto() {
        rechaza(r -> r.setMonto(null), ReglaNegocioException.class);
        rechaza(r -> r.setMonto(BigDecimal.ZERO), ReglaNegocioException.class);
        rechaza(r -> r.setMonto(new BigDecimal("1.001")), ReglaNegocioException.class);
        rechaza(r -> r.setMonto(new BigDecimal("0.50")), ReglaNegocioException.class);
        rechaza(r -> r.setMonto(new BigDecimal("6000.00")), ReglaNegocioException.class);

        recarga.setMontoMinimo(new BigDecimal("50.00"));
        recarga.setMontoMaximo(new BigDecimal("80.00"));
        rechaza(r -> r.setMonto(new BigDecimal("40.00")), ReglaNegocioException.class);
        rechaza(r -> { }, ReglaNegocioException.class);

        recarga.setMontoMinimo(null);
        recarga.setMontoMaximo(null);
        props.setMontoMinimoOperacion(null);
        props.setMontoMaximoOperacion(null);
        saldo.setSaldoDisponible(new BigDecimal("10.00"));
        rechaza(r -> { }, ReglaNegocioException.class);
        saldo.setSaldoDisponible(null);
        rechaza(r -> { }, ReglaNegocioException.class);
        when(saldoRepository.findByCuentaNumeroCuenta(CUENTA)).thenReturn(Optional.empty());
        rechaza(r -> { }, RecursoNoEncontradoException.class);
    }

    @Test
    @DisplayName("Solicitar código de un servicio por RFC usa el teléfono del cliente o uno genérico")
    void solicitarCodigoServicio() {
        when(codigoVerificacionService.emitirCodigo(any())).thenReturn(CodigoVerificacionResponse.builder().correoEnmascarado("c***@x.com").build());
        SolicitarCodigoPagoRequest request = SolicitarCodigoPagoRequest.builder().rfc(RFC).numeroCuenta(CUENTA)
                .idProducto(120).idServicio(10).telefono(" ").referencia("REF").monto(new BigDecimal("10")).build();

        assertThat(servicio.solicitarCodigo(request, USUARIO).getMonto()).isEqualByComparingTo("10.00");
        cliente.setTelefonoMovil(null);
        servicio.solicitarCodigo(request, USUARIO);
        request.setTelefono("55123456789012");
        servicioCfe.setTipoFront(2);
        servicio.solicitarCodigo(request, USUARIO);

        ArgumentCaptor<EmitirCodigo2FARequest> emitidos = ArgumentCaptor.forClass(EmitirCodigo2FARequest.class);
        verify(codigoVerificacionService, times(3)).emitirCodigo(emitidos.capture());
        assertThat(emitidos.getAllValues()).extracting(EmitirCodigo2FARequest::getTelefono)
                .containsExactly("5598765432", "5500000000", "55123456789012");
    }

    @Test
    @DisplayName("Código 2FA ausente o con formato inválido")
    void codigo2fa() {
        rechaza(r -> r.setCodigo(null), ReglaNegocioException.class);
        rechaza(r -> r.setCodigo("12a456"), ReglaNegocioException.class);
    }

    @Test
    @DisplayName("Si el proveedor no da token no se reserva saldo; la unidad configurada debe ser válida")
    void proveedorYUnidad() {
        when(puntoRedClient.obtenerToken()).thenThrow(new PuntoRedAutenticacionException("m", "10", "t"));
        rechaza(r -> { }, ProveedorNoDisponibleException.class);
        verify(pagoTransaccionalService, never()).reservarSaldoYCrearPago(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any());

        doReturn("token").when(puntoRedClient).obtenerToken();
        props.setZonaHoraria(" ");
        props.setUnidad(null);
        rechaza(r -> { }, PuntoRedIntegracionException.class);
        props.setZonaHoraria(null);
        props.setUnidad("AB");
        rechaza(r -> { }, PuntoRedIntegracionException.class);
        props.setZonaHoraria("Zona/Invalida");
        props.setUnidad("U".repeat(26));
        rechaza(r -> { }, PuntoRedIntegracionException.class);
    }

    @Test
    @DisplayName("Carrera de idempotencia: responde con el ganador o propaga si no existe")
    void carreraIdempotencia() {
        DataIntegrityViolationException duplicado = new DataIntegrityViolationException("dup");
        when(pagoTransaccionalService.reservarSaldoYCrearPago(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenThrow(duplicado);
        assertThatThrownBy(() -> servicio.aplicarPago(recargaValida(), USUARIO)).isSameAs(duplicado);

        PagoServicio ganador = PagoServicio.builder().upc("G").curp(CURP).numeroCuenta(CUENTA).idProducto(55)
                .monto(new BigDecimal("100.00")).estatus(EstatusPago.EXITOSO).build();
        when(pagoServicioRepository.findByIdempotencyKey(LLAVE)).thenReturn(Optional.empty(), Optional.of(ganador));
        assertThat(servicio.aplicarPago(recargaValida(), USUARIO).getUpc()).isEqualTo("G");
    }

    @Test
    @DisplayName("Llave de idempotencia existente: solo responde si es la misma operación del mismo titular")
    void idempotencia() {
        PagoServicio existente = PagoServicio.builder().upc("E").curp(CURP).numeroCuenta(CUENTA).idProducto(55)
                .monto(new BigDecimal("100.00")).estatus(EstatusPago.EXITOSO).build();
        when(pagoServicioRepository.findByIdempotencyKey(LLAVE)).thenReturn(Optional.of(existente));

        assertThat(servicio.aplicarPago(recargaValida(), USUARIO).getMensaje()).contains("Idempotente");
        rechaza(r -> r.setNumeroCuenta(null), RecursoDuplicadoException.class);
        rechaza(r -> r.setNumeroCuenta("1111111111"), RecursoDuplicadoException.class);
        rechaza(r -> r.setIdProducto(120), RecursoDuplicadoException.class);
        rechaza(r -> r.setMonto(null), RecursoDuplicadoException.class);
        rechaza(r -> r.setMonto(new BigDecimal("99.00")), RecursoDuplicadoException.class);
        existente.setCurp("OTRA");
        rechaza(r -> { }, AccesoDenegadoException.class);
    }

    @Test
    @DisplayName("Resultados de sendTx: éxito, rechazo con y sin detalle, timeout, respuesta inválida y error genérico")
    void resultadosProveedor() {
        PagoServicio pago = pendienteReservado();
        when(pagoTransaccionalService.aplicarExito(eq(9L), eq(CUENTA), any(), any())).thenReturn(pago);
        when(pagoTransaccionalService.aplicarRechazo(eq(9L), eq(CUENTA), any(), any(), any())).thenReturn(pago);

        when(puntoRedClient.sendTx(any(SendTxPayload.class))).thenReturn(new PuntoRedTxResponse());
        assertThat(servicio.aplicarPago(recargaValida(), USUARIO).getMensaje()).contains("exitosamente");

        when(puntoRedClient.sendTx(any(SendTxPayload.class))).thenThrow(new PuntoRedRechazoException("Rechazo", null, null));
        PagoResponse rechazoSinDetalle = servicio.aplicarPago(recargaValida(), USUARIO);
        assertThat(rechazoSinDetalle.getMensaje()).isEqualTo(ErrorMessage.PUNTORED_TRANSACCION_RECHAZADA.getMensaje());
        verify(pagoTransaccionalService).aplicarRechazo(eq(9L), eq(CUENTA), any(),
                eq(String.valueOf(ErrorMessage.PUNTORED_TRANSACCION_RECHAZADA.getCodigo())), eq("Rechazo"));

        when(puntoRedClient.sendTx(any(SendTxPayload.class))).thenThrow(new PuntoRedRechazoException("06", "Duplicada"));
        servicio.aplicarPago(recargaValida(), USUARIO);
        verify(pagoTransaccionalService).aplicarRechazo(eq(9L), eq(CUENTA), any(), eq("06"), eq("Duplicada"));

        when(puntoRedClient.sendTx(any(SendTxPayload.class))).thenThrow(new PuntoRedTimeoutException("t", "82", "Timeout", "TX82"));
        servicio.aplicarPago(recargaValida(), USUARIO);
        verify(pagoTransaccionalService).registrarTimeout(9L, "TIMEOUT_PROVEEDOR", "TX82");

        when(puntoRedClient.sendTx(any(SendTxPayload.class))).thenThrow(new PuntoRedRespuestaInvalidaException("xml"));
        servicio.aplicarPago(recargaValida(), USUARIO);
        verify(pagoTransaccionalService).registrarTimeout(eq(9L), eq("RESPUESTA_INVALIDA_PROVEEDOR"), isNull());

        when(puntoRedClient.sendTx(any(SendTxPayload.class))).thenThrow(new IllegalStateException("red"));
        assertThat(servicio.aplicarPago(recargaValida(), USUARIO).getMensaje()).contains("Pendiente de confirmación");
        verify(pagoTransaccionalService).registrarTimeout(eq(9L), eq("ERROR_COMUNICACION_PROVEEDOR"), isNull());
    }

    @Test
    @DisplayName("Payload de verificación: pago nulo, producto fuera de catálogo y unidad del pago o configurada")
    void payloadVerificacion() {
        assertThatThrownBy(() -> servicio.armarPayloadDesdePago(null)).isInstanceOf(ReglaNegocioException.class);

        PagoServicio pago = PagoServicio.builder().upc("UPC1").idProducto(999).idServicio(1).telefono("5512345678")
                .referencia("REF").monto(new BigDecimal("10.00")).unidad("PROPIA").build();
        SendTxPayload conUnidad = servicio.armarPayloadDesdePago(pago);
        assertThat(conUnidad.getUnidad()).isEqualTo("PROPIA");
        assertThat(conUnidad.getTipoFront()).isNull();

        pago.setUnidad(null);
        pago.setIdProducto(55);
        SendTxPayload conConfigurada = servicio.armarPayloadDesdePago(pago);
        assertThat(conConfigurada.getUnidad()).isEqualTo("SUCURSAL_CENTRO");
        assertThat(conConfigurada.getTipoFront()).isZero();
    }
}
