package com.kapm.onboarding_clientes.service;

import com.kapm.onboarding_clientes.client.puntored.PuntoRedClient;
import com.kapm.onboarding_clientes.client.puntored.model.PuntoRedMensaje;
import com.kapm.onboarding_clientes.client.puntored.model.PuntoRedTxResponse;
import com.kapm.onboarding_clientes.client.puntored.model.SendTxPayload;
import com.kapm.onboarding_clientes.config.PuntoRedProperties;
import com.kapm.onboarding_clientes.dto.request.AplicarPagoRequest;
import com.kapm.onboarding_clientes.dto.request.EmitirCodigo2FARequest;
import com.kapm.onboarding_clientes.dto.request.SolicitarCodigoPagoRequest;
import com.kapm.onboarding_clientes.dto.request.ValidarCodigo2FARequest;
import com.kapm.onboarding_clientes.dto.response.ClienteResponse;
import com.kapm.onboarding_clientes.dto.response.CodigoVerificacionResponse;
import com.kapm.onboarding_clientes.dto.response.CuentaResponse;
import com.kapm.onboarding_clientes.dto.response.PagoResponse;
import com.kapm.onboarding_clientes.dto.response.SolicitarCodigoPagoResponse;
import com.kapm.onboarding_clientes.entity.Cuenta;
import com.kapm.onboarding_clientes.entity.EstatusCuenta;
import com.kapm.onboarding_clientes.entity.EstatusPago;
import com.kapm.onboarding_clientes.entity.PagoServicio;
import com.kapm.onboarding_clientes.entity.Saldo;
import com.kapm.onboarding_clientes.entity.catalogo.CatalogoProductoPuntoRed;
import com.kapm.onboarding_clientes.exception.ErrorMessage;
import com.kapm.onboarding_clientes.exception.PuntoRedRechazoException;
import com.kapm.onboarding_clientes.exception.PuntoRedTimeoutException;
import com.kapm.onboarding_clientes.exception.ReglaNegocioException;
import com.kapm.onboarding_clientes.repository.CatalogoProductoPuntoRedRepository;
import com.kapm.onboarding_clientes.repository.PagoServicioRepository;
import com.kapm.onboarding_clientes.repository.SaldoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Pruebas Unitarias - Flujo de Pagos y Recargas (Paso 1 y Paso 2)")
class PagoServicioFlowTest {

    private static final String USUARIO = "carlos.mendoza";

    @Mock
    private PagoServicioRepository pagoServicioRepository;

    @Mock
    private CatalogoProductoPuntoRedRepository catalogoProductoRepository;

    @Spy
    private PuntoRedProperties puntoRedProperties = new PuntoRedProperties();

    @Spy
    private UpcGenerator upcGenerator = new UpcGenerator();

    @Mock
    private PuntoRedClient puntoRedClient;

    @Mock
    private ClienteService clienteService;

    @Mock
    private SaldoRepository saldoRepository;

    @Mock
    private CodigoVerificacionService codigoVerificacionService;

    @Mock
    private PagoTransaccionalService pagoTransaccionalService;

    @InjectMocks
    private PagoServicioServiceImpl pagoServicioService;

    private ClienteResponse clienteValido;
    private CatalogoProductoPuntoRed productoRecarga;
    private CatalogoProductoPuntoRed productoServicio;
    private Saldo saldoValido;

    @BeforeEach
    void setUp() {
        lenient().when(clienteService.obtenerCurpPorUsername(USUARIO)).thenReturn("MEHC920824HDFRMN09");
        puntoRedProperties.setUnidad("SUCURSAL_CENTRO");
        puntoRedProperties.setZonaHoraria("America/Mexico_City");

        CuentaResponse cuenta = CuentaResponse.builder()
                .numeroCuenta("4819204812")
                .estatus(EstatusCuenta.ACTIVA)
                .saldo(new BigDecimal("1500.00"))
                .build();

        clienteValido = ClienteResponse.builder()
                .curp("MEHC920824HDFRMN09")
                .rfc("MEHC9208243A8")
                .nombre("Carlos Alberto")
                .apellidoPaterno("Mendoza")
                .correo("carlos.mendoza@email.com")
                .telefonoMovil("5598765432")
                .activo(true)
                .cuenta(cuenta)
                .build();

        productoRecarga = CatalogoProductoPuntoRed.builder()
                .id(1L)
                .idProducto(55)
                .idServicio(2)
                .nombreProducto("Telcel Paquete Amigo Sin Límite 100")
                .nombreServicio("Telcel TAE")
                .tipoFront(0) // Recarga
                .activo(true)
                .build();

        productoServicio = CatalogoProductoPuntoRed.builder()
                .id(2L)
                .idProducto(120)
                .idServicio(10)
                .nombreProducto("CFE Suministrador de Servicios Básicos")
                .nombreServicio("CFE")
                .tipoFront(1) // Servicio
                .activo(true)
                .build();

        saldoValido = Saldo.builder()
                .id(1L)
                .cuenta(Cuenta.builder().id(1L).numeroCuenta("4819204812").saldo(new BigDecimal("1500.00")).build())
                .saldoDisponible(new BigDecimal("1500.00"))
                .saldoContable(new BigDecimal("1500.00"))
                .fechaUltimaActualizacion(LocalDateTime.now())
                .build();
    }

    // =========================================================================
    // PASO 1: POST /api/pagos/solicitar-codigo
    // =========================================================================

    @Nested
    @DisplayName("Paso 1: Solicitar Código 2FA")
    class SolicitarCodigoTests {

        @Test
        @DisplayName("Éxito: Solicitar código para recarga TAE emite código y retorna correo enmascarado")
        void solicitarCodigo_Recarga_Exito() {
            SolicitarCodigoPagoRequest request = SolicitarCodigoPagoRequest.builder()
                    .curp("MEHC920824HDFRMN09")
                    .numeroCuenta("4819204812")
                    .idProducto(55)
                    .idServicio(2)
                    .telefono("5512345678")
                    .monto(new BigDecimal("100.00"))
                    .build();

            when(clienteService.obtenerClientePorCurp("MEHC920824HDFRMN09")).thenReturn(clienteValido);
            when(catalogoProductoRepository.findByIdProducto(55)).thenReturn(Optional.of(productoRecarga));
            when(saldoRepository.findByCuentaNumeroCuenta("4819204812")).thenReturn(Optional.of(saldoValido));
            when(codigoVerificacionService.emitirCodigo(any(EmitirCodigo2FARequest.class))).thenReturn(
                    CodigoVerificacionResponse.builder()
                            .enviado(true)
                            .correoEnmascarado("c***@email.com")
                            .expiraEn(LocalDateTime.now().plusMinutes(5))
                            .mensaje("Código enviado exitosamente")
                            .build()
            );

            SolicitarCodigoPagoResponse response = pagoServicioService.solicitarCodigo(request, USUARIO);

            assertThat(response).isNotNull();
            assertThat(response.getCorreoEnmascarado()).isEqualTo("c***@email.com");
            assertThat(response.getNumeroCuenta()).isEqualTo("4819204812");
            assertThat(response.getMonto()).isEqualByComparingTo(new BigDecimal("100.00"));
            verify(codigoVerificacionService).emitirCodigo(any(EmitirCodigo2FARequest.class));
        }

        @Test
        @DisplayName("Éxito: Solicitar código para pago de servicio con referencia válida")
        void solicitarCodigo_Servicio_Exito() {
            SolicitarCodigoPagoRequest request = SolicitarCodigoPagoRequest.builder()
                    .rfc("MEHC9208243A8")
                    .numeroCuenta("4819204812")
                    .idProducto(120)
                    .idServicio(10)
                    .referencia("REC-CFE-00123")
                    .monto(new BigDecimal("350.50"))
                    .build();

            when(clienteService.obtenerClientePorRfc("MEHC9208243A8")).thenReturn(clienteValido);
            when(catalogoProductoRepository.findByIdProducto(120)).thenReturn(Optional.of(productoServicio));
            when(saldoRepository.findByCuentaNumeroCuenta("4819204812")).thenReturn(Optional.of(saldoValido));
            when(codigoVerificacionService.emitirCodigo(any(EmitirCodigo2FARequest.class))).thenReturn(
                    CodigoVerificacionResponse.builder()
                            .enviado(true)
                            .correoEnmascarado("c***@email.com")
                            .expiraEn(LocalDateTime.now().plusMinutes(5))
                            .mensaje("Código enviado exitosamente")
                            .build()
            );

            SolicitarCodigoPagoResponse response = pagoServicioService.solicitarCodigo(request, USUARIO);

            assertThat(response).isNotNull();
            assertThat(response.getCorreoEnmascarado()).isEqualTo("c***@email.com");
            assertThat(response.getIdProducto()).isEqualTo(120);
        }

        @Test
        @DisplayName("Error: Saldo disponible insuficiente para cubrir el monto del servicio")
        void solicitarCodigo_SaldoInsuficiente_LanzaExcepcion() {
            SolicitarCodigoPagoRequest request = SolicitarCodigoPagoRequest.builder()
                    .curp("MEHC920824HDFRMN09")
                    .numeroCuenta("4819204812")
                    .idProducto(55)
                    .idServicio(2)
                    .telefono("5512345678")
                    .monto(new BigDecimal("2000.00")) // Saldo disponible es 1500.00
                    .build();

            when(clienteService.obtenerClientePorCurp("MEHC920824HDFRMN09")).thenReturn(clienteValido);
            when(catalogoProductoRepository.findByIdProducto(55)).thenReturn(Optional.of(productoRecarga));
            when(saldoRepository.findByCuentaNumeroCuenta("4819204812")).thenReturn(Optional.of(saldoValido));

            assertThatThrownBy(() -> pagoServicioService.solicitarCodigo(request, USUARIO))
                    .isInstanceOf(ReglaNegocioException.class)
                    .hasMessageContaining(ErrorMessage.SALDO_INSUFICIENTE.getMensaje());
        }

        @Test
        @DisplayName("Error: Cliente inactivo no puede solicitar código")
        void solicitarCodigo_ClienteInactivo_LanzaExcepcion() {
            clienteValido.setActivo(false);

            SolicitarCodigoPagoRequest request = SolicitarCodigoPagoRequest.builder()
                    .curp("MEHC920824HDFRMN09")
                    .numeroCuenta("4819204812")
                    .idProducto(55)
                    .idServicio(2)
                    .telefono("5512345678")
                    .monto(new BigDecimal("50.00"))
                    .build();

            when(clienteService.obtenerClientePorCurp("MEHC920824HDFRMN09")).thenReturn(clienteValido);

            assertThatThrownBy(() -> pagoServicioService.solicitarCodigo(request, USUARIO))
                    .isInstanceOf(ReglaNegocioException.class)
                    .hasMessageContaining(ErrorMessage.CLIENTE_INACTIVO.getMensaje());
        }

        @Test
        @DisplayName("Error: Cuenta no pertenece al cliente")
        void solicitarCodigo_CuentaNoPertenece_LanzaExcepcion() {
            SolicitarCodigoPagoRequest request = SolicitarCodigoPagoRequest.builder()
                    .curp("MEHC920824HDFRMN09")
                    .numeroCuenta("9999999999") // Diferente a 4819204812
                    .idProducto(55)
                    .idServicio(2)
                    .telefono("5512345678")
                    .monto(new BigDecimal("50.00"))
                    .build();

            when(clienteService.obtenerClientePorCurp("MEHC920824HDFRMN09")).thenReturn(clienteValido);

            assertThatThrownBy(() -> pagoServicioService.solicitarCodigo(request, USUARIO))
                    .isInstanceOf(ReglaNegocioException.class)
                    .hasMessageContaining(ErrorMessage.CUENTA_NO_PERTENECE_CLIENTE.getMensaje());
        }

        @Test
        @DisplayName("Error: Recarga TAE sin teléfono válido de 10 dígitos es rechazada")
        void solicitarCodigo_RecargaSinTelefono_LanzaExcepcion() {
            SolicitarCodigoPagoRequest request = SolicitarCodigoPagoRequest.builder()
                    .curp("MEHC920824HDFRMN09")
                    .numeroCuenta("4819204812")
                    .idProducto(55)
                    .idServicio(2)
                    .telefono("123") // No cumple 10 dígitos
                    .monto(new BigDecimal("50.00"))
                    .build();

            when(clienteService.obtenerClientePorCurp("MEHC920824HDFRMN09")).thenReturn(clienteValido);
            when(catalogoProductoRepository.findByIdProducto(55)).thenReturn(Optional.of(productoRecarga));

            assertThatThrownBy(() -> pagoServicioService.solicitarCodigo(request, USUARIO))
                    .isInstanceOf(ReglaNegocioException.class)
                    .hasMessageContaining(ErrorMessage.TELEFONO_REQUERIDO_RECARGA.getMensaje());
        }

        @Test
        @DisplayName("Error: Pago de servicio sin referencia es rechazado")
        void solicitarCodigo_ServicioSinReferencia_LanzaExcepcion() {
            SolicitarCodigoPagoRequest request = SolicitarCodigoPagoRequest.builder()
                    .curp("MEHC920824HDFRMN09")
                    .numeroCuenta("4819204812")
                    .idProducto(120)
                    .idServicio(10)
                    .referencia(null) // Falta referencia
                    .monto(new BigDecimal("150.00"))
                    .build();

            when(clienteService.obtenerClientePorCurp("MEHC920824HDFRMN09")).thenReturn(clienteValido);
            when(catalogoProductoRepository.findByIdProducto(120)).thenReturn(Optional.of(productoServicio));

            assertThatThrownBy(() -> pagoServicioService.solicitarCodigo(request, USUARIO))
                    .isInstanceOf(ReglaNegocioException.class)
                    .hasMessageContaining(ErrorMessage.REFERENCIA_REQUERIDA_SERVICIO.getMensaje());
        }

        @Test
        @DisplayName("Error: Producto no perteneciente al catálogo es rechazado")
        void solicitarCodigo_ProductoInexistente_LanzaExcepcion() {
            SolicitarCodigoPagoRequest request = SolicitarCodigoPagoRequest.builder()
                    .curp("MEHC920824HDFRMN09")
                    .numeroCuenta("4819204812")
                    .idProducto(999)
                    .idServicio(1)
                    .telefono("5512345678")
                    .monto(new BigDecimal("50.00"))
                    .build();

            when(clienteService.obtenerClientePorCurp("MEHC920824HDFRMN09")).thenReturn(clienteValido);
            when(catalogoProductoRepository.findByIdProducto(999)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> pagoServicioService.solicitarCodigo(request, USUARIO))
                    .isInstanceOf(ReglaNegocioException.class)
                    .hasMessageContaining(ErrorMessage.PRODUCTO_NO_PERMITIDO.getMensaje());
        }
    }

    // =========================================================================
    // PASO 2: POST /api/pagos/aplicar
    // =========================================================================

    @Nested
    @DisplayName("Paso 2: Aplicar Pago con 2FA e IdempotencyKey")
    class AplicarPagoTests {

        private AplicarPagoRequest requestValido;

        @BeforeEach
        void setUpAplicar() {
            requestValido = AplicarPagoRequest.builder()
                    .curp("MEHC920824HDFRMN09")
                    .numeroCuenta("4819204812")
                    .idProducto(55)
                    .idServicio(2)
                    .telefono("5512345678")
                    .monto(new BigDecimal("100.00"))
                    .codigo("492015")
                    .idempotencyKey("KEY-TX-1001")
                    .build();
        }

        @Test
        @DisplayName("Idempotencia: Si llega el mismo idempotencyKey otra vez, devuelve el resultado previo sin cobrar de nuevo")
        void aplicarPago_Idempotencia_RetornaTransaccionPreviaSinCobrar() {
            PagoServicio previo = PagoServicio.builder()
                    .id(10L)
                    .upc("UPC12345678901")
                    .idempotencyKey("KEY-TX-1001")
                    .curp("MEHC920824HDFRMN09")
                    .numeroCuenta("4819204812")
                    .idProducto(55)
                    .idServicio(2)
                    .monto(new BigDecimal("100.00"))
                    .comision(BigDecimal.ZERO)
                    .estatus(EstatusPago.EXITOSO)
                    .numAutorizacion("AUT-88192")
                    .idTxProveedor("TX-9901")
                    .codigoRespuesta("01")
                    .build();

            when(pagoServicioRepository.findByIdempotencyKey("KEY-TX-1001")).thenReturn(Optional.of(previo));

            PagoResponse response = pagoServicioService.aplicarPago(requestValido, USUARIO);

            assertThat(response).isNotNull();
            assertThat(response.getUpc()).isEqualTo("UPC12345678901");
            assertThat(response.getEstatus()).isEqualTo(EstatusPago.EXITOSO);
            assertThat(response.getMensaje()).contains("Idempotente");

            // NUNCA se debitó saldo ni se llamó a PuntoRed ni se validó 2FA
            verify(pagoTransaccionalService, never()).reservarSaldoYCrearPago(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
            verify(puntoRedClient, never()).sendTx(any());
            verify(codigoVerificacionService, never()).validarCodigo(any());
        }

        @Test
        @DisplayName("Éxito: Flujo completo aprobado por PuntoRed -> reserva saldo, sendTx HTTP y liquidación exitosa")
        void aplicarPago_Exito_LiquidacionCorrecta() {
            when(pagoServicioRepository.findByIdempotencyKey("KEY-TX-1001")).thenReturn(Optional.empty());
            when(clienteService.obtenerClientePorCurp("MEHC920824HDFRMN09")).thenReturn(clienteValido);
            when(catalogoProductoRepository.findByIdProducto(55)).thenReturn(Optional.of(productoRecarga));
            when(saldoRepository.findByCuentaNumeroCuenta("4819204812")).thenReturn(Optional.of(saldoValido));
            when(codigoVerificacionService.validarCodigo(any(ValidarCodigo2FARequest.class))).thenReturn(true);

            PagoServicio pagoPendiente = PagoServicio.builder()
                    .id(1L)
                    .upc("UPC-NEW-12345")
                    .idempotencyKey("KEY-TX-1001")
                    .numeroCuenta("4819204812")
                    .idProducto(55)
                    .idServicio(2)
                    .telefono("5512345678")
                    .monto(new BigDecimal("100.00"))
                    .horaLocal("20261023190000")
                    .unidad("SUCURSAL_CENTRO")
                    .estatus(EstatusPago.PENDIENTE)
                    .build();

            when(pagoTransaccionalService.reservarSaldoYCrearPago(
                    eq("4819204812"), any(), any(), eq(55), eq(2), eq("5512345678"), isNull(), eq(new BigDecimal("100.00")), eq("KEY-TX-1001"), anyString(), anyString()
            )).thenReturn(pagoPendiente);

            PuntoRedTxResponse txAprobada = PuntoRedTxResponse.builder()
                    .idTx("TX-PR-5521")
                    .numAutorizacion("AUT-771829")
                    .comision(new BigDecimal("0.00"))
                    .mensaje(new PuntoRedMensaje("01", "TRANSACCION APROBADA"))
                    .build();

            when(puntoRedClient.sendTx(any(SendTxPayload.class))).thenReturn(txAprobada);

            PagoServicio pagoExitoso = PagoServicio.builder()
                    .id(1L)
                    .upc("UPC-NEW-12345")
                    .idempotencyKey("KEY-TX-1001")
                    .numeroCuenta("4819204812")
                    .idProducto(55)
                    .idServicio(2)
                    .monto(new BigDecimal("100.00"))
                    .estatus(EstatusPago.EXITOSO)
                    .numAutorizacion("AUT-771829")
                    .idTxProveedor("TX-PR-5521")
                    .build();

            when(pagoTransaccionalService.aplicarExito(eq(1L), eq("4819204812"), eq(new BigDecimal("100.00")), eq(txAprobada)))
                    .thenReturn(pagoExitoso);

            PagoResponse response = pagoServicioService.aplicarPago(requestValido, USUARIO);

            assertThat(response).isNotNull();
            assertThat(response.getEstatus()).isEqualTo(EstatusPago.EXITOSO);
            assertThat(response.getNumAutorizacion()).isEqualTo("AUT-771829");
            verify(pagoTransaccionalService).reservarSaldoYCrearPago(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
            verify(puntoRedClient).sendTx(any(SendTxPayload.class));
            verify(pagoTransaccionalService).aplicarExito(any(), any(), any(), any());
        }

        @Test
        @DisplayName("Rechazo confirmado: PuntoRed responde Rechazo -> pasa a RECHAZADO y restituye saldo disponible")
        void aplicarPago_RechazoConfirmado_RestituyeSaldo() {
            when(pagoServicioRepository.findByIdempotencyKey("KEY-TX-1001")).thenReturn(Optional.empty());
            when(clienteService.obtenerClientePorCurp("MEHC920824HDFRMN09")).thenReturn(clienteValido);
            when(catalogoProductoRepository.findByIdProducto(55)).thenReturn(Optional.of(productoRecarga));
            when(saldoRepository.findByCuentaNumeroCuenta("4819204812")).thenReturn(Optional.of(saldoValido));
            when(codigoVerificacionService.validarCodigo(any(ValidarCodigo2FARequest.class))).thenReturn(true);

            PagoServicio pagoPendiente = PagoServicio.builder()
                    .id(2L)
                    .upc("UPC-RECH-12345")
                    .idempotencyKey("KEY-TX-1001")
                    .numeroCuenta("4819204812")
                    .idProducto(55)
                    .idServicio(2)
                    .telefono("5512345678")
                    .monto(new BigDecimal("100.00"))
                    .horaLocal("20261023190000")
                    .unidad("SUCURSAL_CENTRO")
                    .estatus(EstatusPago.PENDIENTE)
                    .build();

            when(pagoTransaccionalService.reservarSaldoYCrearPago(
                    any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()
            )).thenReturn(pagoPendiente);

            when(puntoRedClient.sendTx(any(SendTxPayload.class)))
                    .thenThrow(new PuntoRedRechazoException("99", "SALDO_INSUFICIENTE_DISTRIBUIDOR"));

            PagoServicio pagoRechazado = PagoServicio.builder()
                    .id(2L)
                    .upc("UPC-RECH-12345")
                    .idempotencyKey("KEY-TX-1001")
                    .numeroCuenta("4819204812")
                    .idProducto(55)
                    .idServicio(2)
                    .monto(new BigDecimal("100.00"))
                    .estatus(EstatusPago.RECHAZADO)
                    .codigoRespuesta("99")
                    .textoRespuesta("SALDO_INSUFICIENTE_DISTRIBUIDOR")
                    .build();

            when(pagoTransaccionalService.aplicarRechazo(eq(2L), eq("4819204812"), eq(new BigDecimal("100.00")), eq("99"), anyString()))
                    .thenReturn(pagoRechazado);

            PagoResponse response = pagoServicioService.aplicarPago(requestValido, USUARIO);

            assertThat(response).isNotNull();
            assertThat(response.getEstatus()).isEqualTo(EstatusPago.RECHAZADO);
            assertThat(response.getCodigoRespuesta()).isEqualTo("99");
            verify(pagoTransaccionalService).aplicarRechazo(eq(2L), eq("4819204812"), eq(new BigDecimal("100.00")), eq("99"), anyString());
            verify(pagoTransaccionalService, never()).aplicarExito(any(), any(), any(), any());
        }

        @Test
        @DisplayName("Timeout de red: Falla de comunicación deja pago en PENDIENTE y NO restituye saldo")
        void aplicarPago_Timeout_DejaPendienteSinRestituirSaldo() {
            when(pagoServicioRepository.findByIdempotencyKey("KEY-TX-1001")).thenReturn(Optional.empty());
            when(clienteService.obtenerClientePorCurp("MEHC920824HDFRMN09")).thenReturn(clienteValido);
            when(catalogoProductoRepository.findByIdProducto(55)).thenReturn(Optional.of(productoRecarga));
            when(saldoRepository.findByCuentaNumeroCuenta("4819204812")).thenReturn(Optional.of(saldoValido));
            when(codigoVerificacionService.validarCodigo(any(ValidarCodigo2FARequest.class))).thenReturn(true);

            PagoServicio pagoPendiente = PagoServicio.builder()
                    .id(3L)
                    .upc("UPC-TIMEOUT-123")
                    .idempotencyKey("KEY-TX-1001")
                    .numeroCuenta("4819204812")
                    .idProducto(55)
                    .idServicio(2)
                    .telefono("5512345678")
                    .monto(new BigDecimal("100.00"))
                    .horaLocal("20261023190000")
                    .unidad("SUCURSAL_CENTRO")
                    .estatus(EstatusPago.PENDIENTE)
                    .build();

            when(pagoTransaccionalService.reservarSaldoYCrearPago(
                    any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()
            )).thenReturn(pagoPendiente);

            when(puntoRedClient.sendTx(any(SendTxPayload.class)))
                    .thenThrow(new PuntoRedTimeoutException("Timeout de red al comunicar con PuntoRed", null));

            PagoServicio pagoTimeout = PagoServicio.builder()
                    .id(3L)
                    .upc("UPC-TIMEOUT-123")
                    .idempotencyKey("KEY-TX-1001")
                    .numeroCuenta("4819204812")
                    .idProducto(55)
                    .idServicio(2)
                    .monto(new BigDecimal("100.00"))
                    .estatus(EstatusPago.PENDIENTE)
                    .codigoRespuesta("PENDIENTE")
                    .textoRespuesta("Timeout de red")
                    .build();

            when(pagoTransaccionalService.registrarTimeout(eq(3L), anyString(), any())).thenReturn(pagoTimeout);

            PagoResponse response = pagoServicioService.aplicarPago(requestValido, USUARIO);

            assertThat(response).isNotNull();
            assertThat(response.getEstatus()).isEqualTo(EstatusPago.PENDIENTE);
            assertThat(response.getMensaje()).contains("en proceso");

            // Confirma que NO se llamó a aplicarRechazo (NO se devolvió saldo disponible)
            verify(pagoTransaccionalService, never()).aplicarRechazo(any(), any(), any(), any(), any());
            verify(pagoTransaccionalService).registrarTimeout(eq(3L), anyString(), any());
        }

        @Test
        @DisplayName("Error: Código 2FA inválido o expirado detiene el flujo antes de reservar saldo")
        void aplicarPago_Codigo2FAInvalido_LanzaExcepcion() {
            when(pagoServicioRepository.findByIdempotencyKey("KEY-TX-1001")).thenReturn(Optional.empty());
            when(clienteService.obtenerClientePorCurp("MEHC920824HDFRMN09")).thenReturn(clienteValido);
            when(catalogoProductoRepository.findByIdProducto(55)).thenReturn(Optional.of(productoRecarga));
            when(saldoRepository.findByCuentaNumeroCuenta("4819204812")).thenReturn(Optional.of(saldoValido));

            when(codigoVerificacionService.validarCodigo(any(ValidarCodigo2FARequest.class)))
                    .thenThrow(new ReglaNegocioException(ErrorMessage.CODIGO_2FA_INVALIDO));

            assertThatThrownBy(() -> pagoServicioService.aplicarPago(requestValido, USUARIO))
                    .isInstanceOf(ReglaNegocioException.class)
                    .hasMessageContaining(ErrorMessage.CODIGO_2FA_INVALIDO.getMensaje());

            // NUNCA se debitó saldo ni se llamó a PuntoRed
            verify(pagoTransaccionalService, never()).reservarSaldoYCrearPago(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
            verify(puntoRedClient, never()).sendTx(any());
        }
    }
}
