package com.kapm.onboarding_clientes.service;

import com.kapm.onboarding_clientes.client.puntored.PuntoRedClient;
import com.kapm.onboarding_clientes.client.puntored.model.SendTxPayload;
import com.kapm.onboarding_clientes.config.PuntoRedProperties;
import com.kapm.onboarding_clientes.dto.response.PagoResponse;
import com.kapm.onboarding_clientes.entity.EstatusPago;
import com.kapm.onboarding_clientes.entity.PagoServicio;
import com.kapm.onboarding_clientes.entity.catalogo.CatalogoProductoPuntoRed;
import com.kapm.onboarding_clientes.exception.PuntoRedIntegracionException;
import com.kapm.onboarding_clientes.exception.PuntoRedTimeoutException;
import com.kapm.onboarding_clientes.exception.RecursoNoEncontradoException;
import com.kapm.onboarding_clientes.exception.ReglaNegocioException;
import com.kapm.onboarding_clientes.repository.CatalogoProductoPuntoRedRepository;
import com.kapm.onboarding_clientes.repository.PagoServicioRepository;
import com.kapm.onboarding_clientes.service.VerificacionPagosServiceImpl.Resultado;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("VerificacionPagosServiceImpl (unitarias): casos complementarios")
class VerificacionPagosUnitTest {

    @Mock PagoServicioRepository pagoServicioRepository;
    @Mock CatalogoProductoPuntoRedRepository catalogoRepository;
    @Mock PagoTransaccionalService pagoTransaccionalService;
    @Mock PagoServicioService pagoServicioService;
    @Mock PuntoRedClient puntoRedClient;

    private VerificacionPagosServiceImpl servicio;

    @BeforeEach
    void setUp() {
        servicio = new VerificacionPagosServiceImpl(pagoServicioRepository, catalogoRepository,
                pagoTransaccionalService, pagoServicioService, puntoRedClient, new PuntoRedProperties());
    }

    private static PagoServicio pago(EstatusPago estatus, int minutos, String idTx) {
        return PagoServicio.builder().id(1L).upc("UPC1").numeroCuenta("1234567890").idProducto(55)
                .monto(new BigDecimal("10.00")).estatus(estatus).idTxProveedor(idTx)
                .fechaCreacion(LocalDateTime.now().minusMinutes(minutos)).build();
    }

    private void producto(Integer tipoFront) {
        when(catalogoRepository.findByIdProducto(55)).thenReturn(Optional.of(
                CatalogoProductoPuntoRed.builder().idProducto(55).tipoFront(tipoFront).build()));
    }

    @Test
    @DisplayName("Verificación manual: UPC inexistente o pago ya cerrado")
    void verificarInvalido() {
        when(pagoServicioRepository.findByUpc("UPC1")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> servicio.verificar(" UPC1 ")).isInstanceOf(RecursoNoEncontradoException.class);

        when(pagoServicioRepository.findByUpc("UPC1")).thenReturn(Optional.of(pago(EstatusPago.EXITOSO, 5, null)));
        assertThatThrownBy(() -> servicio.verificar("UPC1")).isInstanceOf(ReglaNegocioException.class);
    }

    @Test
    @DisplayName("Verificación manual de un pago en conciliación fuera de ventana no cambia su estado")
    void verificarEnConciliacion() {
        PagoServicio pago = pago(EstatusPago.EN_CONCILIACION, 2000, " ");
        when(pagoServicioRepository.findByUpc("UPC1")).thenReturn(Optional.of(pago), Optional.empty());

        PagoResponse respuesta = servicio.verificar("UPC1");

        assertThat(respuesta.getMensaje()).endsWith("SIN_CAMBIO");
        verify(pagoTransaccionalService, never()).marcarEnConciliacion(any());
    }

    @Test
    @DisplayName("Verificación manual de un pago pendiente con la respuesta actualizada")
    void verificarPendiente() {
        PagoServicio pago = pago(EstatusPago.PENDIENTE, 5, "TX");
        PagoServicio actualizado = pago(EstatusPago.EXITOSO, 5, "TX");
        when(pagoServicioRepository.findByUpc("UPC1")).thenReturn(Optional.of(pago), Optional.of(actualizado));
        when(puntoRedClient.confirmTx("TX")).thenThrow(new PuntoRedIntegracionException("caído"));
        when(pagoTransaccionalService.registrarIntentoVerificacion(1L)).thenReturn(1);

        assertThat(servicio.verificar("UPC1").getEstatus()).isEqualTo(EstatusPago.EXITOSO);
    }

    @Test
    @DisplayName("Servicio (tipoFront 2) dentro de su ventana larga reenvía; timeout sin ID_TX no lo registra")
    void reenvioServicioTimeoutSinIdTx() {
        producto(2);
        when(pagoServicioService.armarPayloadDesdePago(any())).thenReturn(new SendTxPayload());
        when(puntoRedClient.sendTx(any())).thenThrow(new PuntoRedTimeoutException("timeout"));
        when(pagoTransaccionalService.registrarIntentoVerificacion(1L)).thenReturn(1);

        assertThat(servicio.verificarPago(pago(EstatusPago.PENDIENTE, 60, null))).isEqualTo(Resultado.SIN_CAMBIO);
        verify(pagoTransaccionalService, never()).registrarTimeout(any(), anyString(), anyString());
    }

    @Test
    @DisplayName("Recarga (tipoFront 0) fuera de la ventana corta se escala")
    void recargaFueraDeVentana() {
        producto(0);
        assertThat(servicio.verificarPago(pago(EstatusPago.PENDIENTE, 60, null))).isEqualTo(Resultado.ESCALADO);
        verify(puntoRedClient, never()).sendTx(any());
    }

    @Test
    @DisplayName("Tipo de producto desconocido usa la ventana corta; error genérico del proveedor registra intento")
    void errorGenericoEnReenvio() {
        producto(null);
        when(pagoServicioService.armarPayloadDesdePago(any())).thenReturn(new SendTxPayload());
        when(puntoRedClient.sendTx(any())).thenThrow(new PuntoRedIntegracionException("respuesta rara"));
        when(pagoTransaccionalService.registrarIntentoVerificacion(1L)).thenReturn(5);

        assertThat(servicio.verificarPago(pago(EstatusPago.PENDIENTE, 1, null))).isEqualTo(Resultado.ESCALADO);
        verify(pagoTransaccionalService).marcarEnConciliacion(1L);
    }
}
