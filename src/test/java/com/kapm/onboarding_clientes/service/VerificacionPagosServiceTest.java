package com.kapm.onboarding_clientes.service;

import com.kapm.onboarding_clientes.client.puntored.PuntoRedClient;
import com.kapm.onboarding_clientes.client.puntored.model.ConfirmacionTx;
import com.kapm.onboarding_clientes.client.puntored.model.PuntoRedMensaje;
import com.kapm.onboarding_clientes.client.puntored.model.PuntoRedTxResponse;
import com.kapm.onboarding_clientes.client.puntored.model.SendTxPayload;
import com.kapm.onboarding_clientes.config.PuntoRedProperties;
import com.kapm.onboarding_clientes.entity.EstatusPago;
import com.kapm.onboarding_clientes.entity.PagoServicio;
import com.kapm.onboarding_clientes.entity.catalogo.CatalogoProductoPuntoRed;
import com.kapm.onboarding_clientes.exception.*;
import com.kapm.onboarding_clientes.repository.CatalogoProductoPuntoRedRepository;
import com.kapm.onboarding_clientes.repository.PagoServicioRepository;
import com.kapm.onboarding_clientes.service.VerificacionPagosServiceImpl.Resultado;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Pruebas Unitarias - Verificación automática de pagos con resultado desconocido")
class VerificacionPagosServiceTest {

    private static final String CUENTA = "4819204812";
    private static final BigDecimal MONTO = new BigDecimal("100.00");

    @Mock private PagoServicioRepository pagoServicioRepository;
    @Mock private CatalogoProductoPuntoRedRepository catalogoRepository;
    @Mock private PagoTransaccionalService pagoTransaccionalService;
    @Mock private PagoServicioService pagoServicioService;
    @Mock private PuntoRedClient puntoRedClient;

    private PuntoRedProperties properties;
    private VerificacionPagosServiceImpl servicio;

    @BeforeEach
    void setUp() {
        properties = new PuntoRedProperties();
        servicio = new VerificacionPagosServiceImpl(pagoServicioRepository, catalogoRepository,
                pagoTransaccionalService, pagoServicioService, puntoRedClient, properties);
    }

    private PagoServicio pago(long id, int minutosDeAntiguedad, String idTx) {
        return PagoServicio.builder().id(id).upc("UPC-" + id).numeroCuenta(CUENTA).idProducto(55).idServicio(2)
                .monto(MONTO).estatus(EstatusPago.PENDIENTE).idTxProveedor(idTx)
                .fechaCreacion(LocalDateTime.now().minusMinutes(minutosDeAntiguedad)).build();
    }

    private void productoConTipoFront(Integer tipoFront) {
        when(catalogoRepository.findByIdProducto(55)).thenReturn(Optional.of(
                CatalogoProductoPuntoRed.builder().idProducto(55).idServicio(2).tipoFront(tipoFront).build()));
    }

    private void payloadDisponible() {
        when(pagoServicioService.armarPayloadDesdePago(any())).thenReturn(SendTxPayload.builder().upc("UPC-1").build());
    }

    @Nested
    @DisplayName("Con ID_TX: confirmTx")
    class ConConfirmTx {

        @Test
        @DisplayName("01 APLICADA -> liquida el saldo contable con la autorización del proveedor")
        void aplicada_Liquida() {
            PagoServicio pago = pago(1, 5, "291437188");
            PuntoRedTxResponse respuesta = PuntoRedTxResponse.builder().numAutorizacion("100024988")
                    .mensaje(new PuntoRedMensaje("01", "Transaccion Aplicada")).build();
            when(puntoRedClient.confirmTx("291437188")).thenReturn(new ConfirmacionTx(ConfirmacionTx.Estado.APLICADA, respuesta));

            assertThat(servicio.verificarPago(pago)).isEqualTo(Resultado.LIQUIDADO);
            verify(pagoTransaccionalService).aplicarExito(1L, CUENTA, MONTO, respuesta);
            verify(puntoRedClient, never()).sendTx(any());
        }

        @Test
        @DisplayName("03 NO_ENCONTRADA -> libera la reserva")
        void noEncontrada_Libera() {
            PagoServicio pago = pago(1, 5, "291437188");
            when(puntoRedClient.confirmTx("291437188")).thenReturn(new ConfirmacionTx(ConfirmacionTx.Estado.NO_ENCONTRADA, null));

            assertThat(servicio.verificarPago(pago)).isEqualTo(Resultado.LIBERADO);
            verify(pagoTransaccionalService).aplicarRechazo(eq(1L), eq(CUENTA), eq(MONTO), eq("03"), anyString());
        }

        @Test
        @DisplayName("04 PENDIENTE -> no mueve saldo; escala al agotar intentos")
        void pendiente_EsperaYEscalaAlAgotarIntentos() {
            PagoServicio pago = pago(1, 5, "291437188");
            when(puntoRedClient.confirmTx("291437188")).thenReturn(new ConfirmacionTx(ConfirmacionTx.Estado.PENDIENTE, null));
            when(pagoTransaccionalService.registrarIntentoVerificacion(1L)).thenReturn(1, properties.getMaxIntentosVerificacion());

            assertThat(servicio.verificarPago(pago)).isEqualTo(Resultado.SIN_CAMBIO);
            assertThat(servicio.verificarPago(pago)).isEqualTo(Resultado.ESCALADO);
            verify(pagoTransaccionalService).marcarEnConciliacion(1L);
            verify(pagoTransaccionalService, never()).aplicarRechazo(any(), any(), any(), any(), any());
            verify(pagoTransaccionalService, never()).aplicarExito(any(), any(), any(), any());
        }

        @Test
        @DisplayName("Más de 24 h: confirmTx ya no es válido -> conciliación manual sin consultar")
        void vencido_Escala() {
            PagoServicio pago = pago(1, 25 * 60, "291437188");

            assertThat(servicio.verificarPago(pago)).isEqualTo(Resultado.ESCALADO);
            verify(puntoRedClient, never()).confirmTx(any());
        }
    }

    @Nested
    @DisplayName("Sin ID_TX: reenvío del mismo UPC")
    class ReenvioMismoUpc {

        @Test
        @DisplayName("01 en el reenvío -> liquida")
        void reenvioAprobado_Liquida() {
            productoConTipoFront(0);
            payloadDisponible();
            PuntoRedTxResponse ok = PuntoRedTxResponse.builder().mensaje(new PuntoRedMensaje("01", "OK")).build();
            when(puntoRedClient.sendTx(any())).thenReturn(ok);

            assertThat(servicio.verificarPago(pago(1, 5, null))).isEqualTo(Resultado.LIQUIDADO);
            verify(pagoTransaccionalService).aplicarExito(1L, CUENTA, MONTO, ok);
        }

        @Test
        @DisplayName("06 en el reenvío del mismo UPC -> la original SÍ se aplicó: liquida (nunca devuelve saldo)")
        void reenvioDuplicado_LiquidaNoDevuelve() {
            productoConTipoFront(0);
            payloadDisponible();
            when(puntoRedClient.sendTx(any())).thenThrow(new PuntoRedTransaccionDuplicadaException("Aplicada anteriormente [123]"));

            assertThat(servicio.verificarPago(pago(1, 5, null))).isEqualTo(Resultado.LIQUIDADO);

            ArgumentCaptor<PuntoRedTxResponse> captor = ArgumentCaptor.forClass(PuntoRedTxResponse.class);
            verify(pagoTransaccionalService).aplicarExito(eq(1L), eq(CUENTA), eq(MONTO), captor.capture());
            assertThat(captor.getValue().getMensaje().getCodigo()).isEqualTo("06");
            verify(pagoTransaccionalService, never()).aplicarRechazo(any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("Rechazo transaccional en el reenvío (p. ej. 02) -> libera la reserva")
        void reenvioRechazado_Libera() {
            productoConTipoFront(0);
            payloadDisponible();
            when(puntoRedClient.sendTx(any())).thenThrow(new PuntoRedRechazoException("02", "Saldo insuficiente"));

            assertThat(servicio.verificarPago(pago(1, 5, null))).isEqualTo(Resultado.LIBERADO);
            verify(pagoTransaccionalService).aplicarRechazo(1L, CUENTA, MONTO, "02", "Saldo insuficiente");
        }

        @Test
        @DisplayName("Rechazo de AUTENTICACIÓN en el reenvío -> nunca libera saldo, escala")
        void reenvioAuthFallida_EscalaSinLiberar() {
            productoConTipoFront(0);
            payloadDisponible();
            when(puntoRedClient.sendTx(any())).thenThrow(new PuntoRedAutenticacionException("auth", "403", "Forbidden"));

            assertThat(servicio.verificarPago(pago(1, 5, null))).isEqualTo(Resultado.ESCALADO);
            verify(pagoTransaccionalService, never()).aplicarRechazo(any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("CODIGO 82 en el reenvío -> guarda el ID_TX para confirmTx y no mueve saldo")
        void reenvioCodigo82_GuardaIdTx() {
            productoConTipoFront(0);
            payloadDisponible();
            when(puntoRedClient.sendTx(any())).thenThrow(new PuntoRedTimeoutException("82", "82", "Tiempo", "555"));
            when(pagoTransaccionalService.registrarIntentoVerificacion(1L)).thenReturn(1);

            assertThat(servicio.verificarPago(pago(1, 5, null))).isEqualTo(Resultado.SIN_CAMBIO);
            verify(pagoTransaccionalService).registrarTimeout(1L, "TIMEOUT_REENVIO", "555");
            verify(pagoTransaccionalService, never()).aplicarRechazo(any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("Recarga fuera de la ventana de 15 min -> NO reenvía (sería una venta nueva): escala")
        void recargaFueraDeVentana_NoReenvia() {
            productoConTipoFront(0);

            assertThat(servicio.verificarPago(pago(1, 11, null))).isEqualTo(Resultado.ESCALADO);
            verify(puntoRedClient, never()).sendTx(any());
        }

        @Test
        @DisplayName("Servicio (tipoFront 1) a 2 h -> dentro de la ventana de 24 h, sí reenvía")
        void servicioDentroDeVentana_Reenvia() {
            productoConTipoFront(1);
            payloadDisponible();
            when(puntoRedClient.sendTx(any())).thenReturn(PuntoRedTxResponse.builder().mensaje(new PuntoRedMensaje("01", "OK")).build());

            assertThat(servicio.verificarPago(pago(1, 120, null))).isEqualTo(Resultado.LIQUIDADO);
        }

        @Test
        @DisplayName("Producto retirado del catálogo -> se asume la ventana corta (conservadora)")
        void productoDesconocido_VentanaCorta() {
            when(catalogoRepository.findByIdProducto(55)).thenReturn(Optional.empty());

            assertThat(servicio.verificarPago(pago(1, 120, null))).isEqualTo(Resultado.ESCALADO);
            verify(puntoRedClient, never()).sendTx(any());
        }
    }

    @Test
    @DisplayName("verificarPendientes: procesa todos aunque uno falle y cuenta solo los que cambiaron")
    void verificarPendientes_ToleraFallas() {
        PagoServicio uno = pago(1, 5, "A");
        PagoServicio dos = pago(2, 5, "B");
        PagoServicio tres = pago(3, 5, "C");
        when(pagoServicioRepository.findByEstatusAndFechaCreacionBefore(eq(EstatusPago.PENDIENTE), any()))
                .thenReturn(List.of(uno, dos, tres));
        when(puntoRedClient.confirmTx("A")).thenReturn(new ConfirmacionTx(ConfirmacionTx.Estado.NO_ENCONTRADA, null));
        when(puntoRedClient.confirmTx("B")).thenThrow(new RuntimeException("falla inesperada"));
        when(puntoRedClient.confirmTx("C")).thenReturn(new ConfirmacionTx(ConfirmacionTx.Estado.PENDIENTE, null));
        when(pagoTransaccionalService.registrarIntentoVerificacion(3L)).thenReturn(1);

        assertThat(servicio.verificarPendientes()).isEqualTo(1);
        verify(puntoRedClient).confirmTx("C");
    }

    @Test
    @DisplayName("verificar(upc) manual rechaza pagos ya cerrados")
    void verificarManual_PagoCerrado() {
        PagoServicio cerrado = pago(1, 5, null);
        cerrado.setEstatus(EstatusPago.EXITOSO);
        when(pagoServicioRepository.findByUpc("UPC-1")).thenReturn(Optional.of(cerrado));

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> servicio.verificar("UPC-1"))
                .isInstanceOf(ReglaNegocioException.class);
        verifyNoInteractions(puntoRedClient);
    }
}
