package com.kapm.onboarding_clientes.service;

import com.kapm.onboarding_clientes.client.puntored.PuntoRedClient;
import com.kapm.onboarding_clientes.config.PuntoRedProperties;
import com.kapm.onboarding_clientes.dto.request.AplicarPagoRequest;
import com.kapm.onboarding_clientes.dto.request.ValidarCodigo2FARequest;
import com.kapm.onboarding_clientes.dto.response.ClienteResponse;
import com.kapm.onboarding_clientes.dto.response.CuentaResponse;
import com.kapm.onboarding_clientes.dto.response.PagoResponse;
import com.kapm.onboarding_clientes.entity.Cuenta;
import com.kapm.onboarding_clientes.entity.EstatusCuenta;
import com.kapm.onboarding_clientes.entity.EstatusPago;
import com.kapm.onboarding_clientes.entity.PagoServicio;
import com.kapm.onboarding_clientes.entity.Saldo;
import com.kapm.onboarding_clientes.entity.catalogo.CatalogoProductoPuntoRed;
import com.kapm.onboarding_clientes.exception.AccesoDenegadoException;
import com.kapm.onboarding_clientes.exception.ErrorMessage;
import com.kapm.onboarding_clientes.exception.ProveedorNoDisponibleException;
import com.kapm.onboarding_clientes.exception.PuntoRedRespuestaInvalidaException;
import com.kapm.onboarding_clientes.exception.PuntoRedTimeoutException;
import com.kapm.onboarding_clientes.exception.RecursoDuplicadoException;
import com.kapm.onboarding_clientes.exception.ReglaNegocioException;
import com.kapm.onboarding_clientes.repository.CatalogoProductoPuntoRedRepository;
import com.kapm.onboarding_clientes.repository.PagoServicioRepository;
import com.kapm.onboarding_clientes.repository.SaldoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Pruebas Unitarias - Reglas de seguridad e integridad del flujo de pago")
class PagoSeguridadFlowTest {

    private static final String USUARIO = "carlos.mendoza";

    @Mock private PagoServicioRepository pagoServicioRepository;
    @Mock private CatalogoProductoPuntoRedRepository catalogoProductoRepository;
    @Spy private PuntoRedProperties puntoRedProperties = new PuntoRedProperties();
    @Spy private UpcGenerator upcGenerator = new UpcGenerator();
    @Mock private PuntoRedClient puntoRedClient;
    @Mock private ClienteService clienteService;
    @Mock private SaldoRepository saldoRepository;
    @Mock private CodigoVerificacionService codigoVerificacionService;
    @Mock private PagoTransaccionalService pagoTransaccionalService;

    @InjectMocks
    private PagoServicioServiceImpl pagoServicioService;

    private AplicarPagoRequest request;
    private CatalogoProductoPuntoRed producto;

    @BeforeEach
    void setUp() {
        lenient().when(clienteService.obtenerCurpPorUsername(USUARIO)).thenReturn("MEHC920824HDFRMN09");
        puntoRedProperties.setUnidad("SUCURSAL_CENTRO");

        producto = CatalogoProductoPuntoRed.builder()
                .idProducto(55).idServicio(2).nombreProducto("Telcel 100").nombreServicio("Telcel TAE")
                .tipoFront(0).activo(true).build();

        request = AplicarPagoRequest.builder()
                .curp("MEHC920824HDFRMN09").numeroCuenta("4819204812")
                .idProducto(55).idServicio(2).telefono("5512345678")
                .monto(new BigDecimal("100.00")).codigo("492015").idempotencyKey("KEY-SEG-1")
                .build();
    }

    private void prepararValidacionesOk() {
        ClienteResponse cliente = ClienteResponse.builder()
                .curp("MEHC920824HDFRMN09").activo(true)
                .cuenta(CuentaResponse.builder().numeroCuenta("4819204812").estatus(EstatusCuenta.ACTIVA).build())
                .build();
        Saldo saldo = Saldo.builder()
                .cuenta(Cuenta.builder().numeroCuenta("4819204812").build())
                .saldoDisponible(new BigDecimal("1500.00")).saldoContable(new BigDecimal("1500.00"))
                .fechaUltimaActualizacion(LocalDateTime.now()).build();

        when(pagoServicioRepository.findByIdempotencyKey("KEY-SEG-1")).thenReturn(Optional.empty());
        when(clienteService.obtenerClientePorCurp("MEHC920824HDFRMN09")).thenReturn(cliente);
        when(catalogoProductoRepository.findByIdProducto(55)).thenReturn(Optional.of(producto));
        lenient().when(saldoRepository.findByCuentaNumeroCuenta("4819204812")).thenReturn(Optional.of(saldo));
    }

    private PagoServicio pagoPendiente() {
        return PagoServicio.builder().id(7L).upc("UPC-SEG-0001").idempotencyKey("KEY-SEG-1").curp("MEHC920824HDFRMN09")
                .numeroCuenta("4819204812").idProducto(55).idServicio(2).telefono("5512345678")
                .monto(new BigDecimal("100.00")).horaLocal("20261028120000").unidad("SUCURSAL_CENTRO")
                .estatus(EstatusPago.PENDIENTE).build();
    }

    @Test
    @DisplayName("Monto manipulado: producto de monto fijo $100 rechaza $150 antes de validar 2FA o tocar saldo")
    void montoFueraDeRangoDelProducto_SeRechaza() {
        producto.setMontoMinimo(new BigDecimal("100.00"));
        producto.setMontoMaximo(new BigDecimal("100.00"));
        request.setMonto(new BigDecimal("150.00"));
        prepararValidacionesOk();

        assertThatThrownBy(() -> pagoServicioService.aplicarPago(request, USUARIO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage(ErrorMessage.MONTO_FUERA_DE_RANGO.getMensaje());

        verify(codigoVerificacionService, never()).validarCodigo(any());
        verify(pagoTransaccionalService, never()).reservarSaldoYCrearPago(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Monto por encima del límite global cuando el producto no define límites")
    void montoSobreLimiteGlobal_SeRechaza() {
        request.setMonto(new BigDecimal("5000.01"));
        prepararValidacionesOk();

        assertThatThrownBy(() -> pagoServicioService.aplicarPago(request, USUARIO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage(ErrorMessage.MONTO_FUERA_DE_RANGO.getMensaje());
    }

    @Test
    @DisplayName("IdempotencyKey reutilizada con otro monto: 409 y no se expone el pago original")
    void llaveReutilizadaConOtraOperacion_Conflicto() {
        PagoServicio previo = pagoPendiente();
        previo.setMonto(new BigDecimal("50.00"));
        when(pagoServicioRepository.findByIdempotencyKey("KEY-SEG-1")).thenReturn(Optional.of(previo));

        assertThatThrownBy(() -> pagoServicioService.aplicarPago(request, USUARIO))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessage(ErrorMessage.IDEMPOTENCY_KEY_REUTILIZADA.getMensaje());
    }

    @Test
    @DisplayName("Proveedor caído al obtener token: 503 sin reservar saldo (la transacción nunca se envió)")
    void tokenNoDisponible_NoReservaSaldo() {
        prepararValidacionesOk();
        when(codigoVerificacionService.validarCodigo(any(ValidarCodigo2FARequest.class))).thenReturn(true);
        when(puntoRedClient.obtenerToken()).thenThrow(new PuntoRedTimeoutException("timeout auth"));

        assertThatThrownBy(() -> pagoServicioService.aplicarPago(request, USUARIO))
                .isInstanceOf(ProveedorNoDisponibleException.class);

        verify(pagoTransaccionalService, never()).reservarSaldoYCrearPago(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
        verify(puntoRedClient, never()).sendTx(any());
    }

    @Test
    @DisplayName("Carrera con la misma llave: el perdedor recibe el pago ganador y nunca llama a sendTx")
    void carreraMismaLlave_DevuelveGanador() {
        prepararValidacionesOk();
        when(codigoVerificacionService.validarCodigo(any(ValidarCodigo2FARequest.class))).thenReturn(true);
        when(pagoTransaccionalService.reservarSaldoYCrearPago(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenThrow(new DataIntegrityViolationException("uk idempotency_key"));
        when(pagoServicioRepository.findByIdempotencyKey("KEY-SEG-1"))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(pagoPendiente()));

        PagoResponse response = pagoServicioService.aplicarPago(request, USUARIO);

        assertThat(response.getUpc()).isEqualTo("UPC-SEG-0001");
        assertThat(response.getMensaje()).contains("Idempotente");
        verify(puntoRedClient, never()).sendTx(any());
    }

    @Test
    @DisplayName("Respuesta ilegible del proveedor: resultado desconocido, queda PENDIENTE y no se devuelve saldo")
    void respuestaInvalida_QuedaPendienteSinDevolverSaldo() {
        prepararValidacionesOk();
        when(codigoVerificacionService.validarCodigo(any(ValidarCodigo2FARequest.class))).thenReturn(true);
        when(pagoTransaccionalService.reservarSaldoYCrearPago(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(pagoPendiente());
        when(puntoRedClient.sendTx(any())).thenThrow(new PuntoRedRespuestaInvalidaException("XML ilegible"));
        when(pagoTransaccionalService.registrarTimeout(eq(7L), eq("RESPUESTA_INVALIDA_PROVEEDOR"), isNull())).thenReturn(pagoPendiente());

        PagoResponse response = pagoServicioService.aplicarPago(request, USUARIO);

        assertThat(response.getEstatus()).isEqualTo(EstatusPago.PENDIENTE);
        verify(pagoTransaccionalService, never()).aplicarRechazo(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Suplantación: el body apunta a un cliente distinto al titular del token -> 403 antes de 2FA y saldo")
    void clienteDistintoAlTitular_AccesoDenegado() {
        when(clienteService.obtenerCurpPorUsername(USUARIO)).thenReturn("OTRO920824HDFRMN01");
        when(pagoServicioRepository.findByIdempotencyKey("KEY-SEG-1")).thenReturn(Optional.empty());
        when(clienteService.obtenerClientePorCurp("MEHC920824HDFRMN09")).thenReturn(ClienteResponse.builder()
                .curp("MEHC920824HDFRMN09").activo(true).build());

        assertThatThrownBy(() -> pagoServicioService.aplicarPago(request, USUARIO))
                .isInstanceOf(AccesoDenegadoException.class);

        verify(codigoVerificacionService, never()).validarCodigo(any());
        verify(pagoTransaccionalService, never()).reservarSaldoYCrearPago(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Idempotencia: una llave de otro titular no devuelve su pago (403)")
    void llaveDeOtroTitular_AccesoDenegado() {
        PagoServicio ajeno = pagoPendiente();
        ajeno.setCurp("OTRO920824HDFRMN01");
        when(pagoServicioRepository.findByIdempotencyKey("KEY-SEG-1")).thenReturn(Optional.of(ajeno));

        assertThatThrownBy(() -> pagoServicioService.aplicarPago(request, USUARIO))
                .isInstanceOf(AccesoDenegadoException.class);
    }
}
