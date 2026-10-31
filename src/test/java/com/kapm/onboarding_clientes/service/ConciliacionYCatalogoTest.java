package com.kapm.onboarding_clientes.service;

import com.kapm.onboarding_clientes.client.puntored.PuntoRedClient;
import com.kapm.onboarding_clientes.client.puntored.model.PuntoRedProducto;
import com.kapm.onboarding_clientes.client.puntored.model.PuntoRedProductosResponse;
import com.kapm.onboarding_clientes.client.puntored.model.PuntoRedTxResponse;
import com.kapm.onboarding_clientes.config.PuntoRedProperties;
import com.kapm.onboarding_clientes.dto.request.ResolverConciliacionRequest;
import com.kapm.onboarding_clientes.entity.EstatusPago;
import com.kapm.onboarding_clientes.entity.PagoServicio;
import com.kapm.onboarding_clientes.entity.catalogo.CatalogoProductoPuntoRed;
import com.kapm.onboarding_clientes.exception.ReglaNegocioException;
import com.kapm.onboarding_clientes.repository.CatalogoProductoPuntoRedRepository;
import com.kapm.onboarding_clientes.repository.PagoServicioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Pruebas Unitarias - Conciliación de pagos y sincronización de catálogo")
class ConciliacionYCatalogoTest {

    @Mock private PagoServicioRepository pagoServicioRepository;
    @Mock private PagoTransaccionalService pagoTransaccionalService;
    @Mock private PuntoRedClient puntoRedClient;
    @Mock private CatalogoProductoPuntoRedRepository catalogoRepository;
    @Mock private CuotaConsultaProveedorService cuotaService;

    private PagoServicio pago(long id) {
        return PagoServicio.builder().id(id).upc("UPC-" + id).numeroCuenta("4819204812")
                .idProducto(55).monto(new BigDecimal("100.00")).estatus(EstatusPago.EN_CONCILIACION).build();
    }

    @Nested
    @DisplayName("Conciliación")
    class Conciliacion {

        private ConciliacionPagosServiceImpl servicio() {
            return new ConciliacionPagosServiceImpl(pagoServicioRepository, pagoTransaccionalService);
        }

        @Test
        @DisplayName("Proveedor confirmó la transacción: se liquida con la autorización indicada")
        void resolverProcesada_Liquida() {
            PagoServicio p = pago(9);
            when(pagoServicioRepository.findByUpc("UPC-9")).thenReturn(Optional.of(p));
            when(pagoTransaccionalService.aplicarExito(eq(9L), eq("4819204812"), any(), any())).thenReturn(p);

            servicio().resolver(ResolverConciliacionRequest.builder()
                    .upc("UPC-9").procesadaPorProveedor(true).numAutorizacion("AUT-77").build());

            ArgumentCaptor<PuntoRedTxResponse> captor = ArgumentCaptor.forClass(PuntoRedTxResponse.class);
            verify(pagoTransaccionalService).aplicarExito(eq(9L), eq("4819204812"), eq(new BigDecimal("100.00")), captor.capture());
            assertThat(captor.getValue().getNumAutorizacion()).isEqualTo("AUT-77");
            verify(pagoTransaccionalService, never()).aplicarRechazo(any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("Proveedor confirmó que NO la procesó: se libera la reserva")
        void resolverNoProcesada_LiberaReserva() {
            PagoServicio p = pago(10);
            when(pagoServicioRepository.findByUpc("UPC-10")).thenReturn(Optional.of(p));
            when(pagoTransaccionalService.aplicarRechazo(eq(10L), any(), any(), any(), any())).thenReturn(p);

            servicio().resolver(ResolverConciliacionRequest.builder().upc("UPC-10").procesadaPorProveedor(false).build());

            verify(pagoTransaccionalService).aplicarRechazo(eq(10L), eq("4819204812"), eq(new BigDecimal("100.00")), any(), any());
            verify(pagoTransaccionalService, never()).aplicarExito(any(), any(), any(), any());
        }
    }

    @Nested
    @DisplayName("Sincronización de catálogo")
    class Catalogo {

        private CatalogoPuntoRedSyncServiceImpl servicio() {
            return new CatalogoPuntoRedSyncServiceImpl(puntoRedClient, catalogoRepository, cuotaService, new PuntoRedProperties());
        }

        private CatalogoProductoPuntoRed activo() {
            return CatalogoProductoPuntoRed.builder().idProducto(1).idServicio(1).tipoFront(0).activo(true).build();
        }

        private PuntoRedProducto remoto(int idProducto, int idServicio, int tipoFront) {
            return PuntoRedProducto.builder().idProducto(idProducto).idServicio(idServicio)
                    .producto("P" + idProducto).servicio("S" + idServicio).tipoFront(tipoFront).build();
        }

        @Test
        @DisplayName("Inserta nuevos, actualiza existentes conservando límites de monto y desactiva los retirados")
        void sincroniza() {
            CatalogoProductoPuntoRed existente = CatalogoProductoPuntoRed.builder().idProducto(55).idServicio(1)
                    .nombreProducto("viejo").nombreServicio("viejo").tipoFront(0).activo(true)
                    .montoMinimo(new BigDecimal("100.00")).montoMaximo(new BigDecimal("100.00")).build();
            CatalogoProductoPuntoRed retirado = CatalogoProductoPuntoRed.builder().idProducto(99).idServicio(9)
                    .nombreProducto("x").nombreServicio("x").tipoFront(1).activo(true).build();

            PuntoRedProductosResponse respuesta = new PuntoRedProductosResponse();
            respuesta.setProductos(new ArrayList<>(List.of(remoto(55, 2, 0), remoto(120, 10, 1))));
            when(cuotaService.reservarConsulta(CatalogoPuntoRedSyncServiceImpl.OPERACION, 3)).thenReturn(true);
            when(puntoRedClient.getProductList()).thenReturn(respuesta);
            when(catalogoRepository.findAll()).thenReturn(List.of(existente, retirado));

            int activos = servicio().sincronizar(false);

            assertThat(activos).isEqualTo(2);
            assertThat(existente.getIdServicio()).isEqualTo(2);
            assertThat(existente.getMontoMaximo()).isEqualByComparingTo("100.00");
            assertThat(retirado.getActivo()).isFalse();
            verify(catalogoRepository, times(3)).save(any());
            verify(cuotaService).registrarExito(CatalogoPuntoRedSyncServiceImpl.OPERACION);
        }

        @Test
        @DisplayName("Catálogo remoto vacío no desactiva el catálogo local")
        void catalogoVacio_NoDesactiva() {
            when(cuotaService.reservarConsulta(any(), anyInt())).thenReturn(true);
            when(puntoRedClient.getProductList()).thenReturn(new PuntoRedProductosResponse());

            servicio().sincronizar(false);

            verify(catalogoRepository, never()).save(any());
            verify(cuotaService, never()).registrarExito(any());
        }

        @Test
        @DisplayName("Sincronización reciente (< 20 h) con catálogo local poblado: no gasta cupo ni llama al proveedor")
        void sincronizacionReciente_Omite() {
            when(catalogoRepository.findByActivoTrue()).thenReturn(List.of(activo()));
            when(cuotaService.ultimaConsultaExitosa(any())).thenReturn(Optional.of(LocalDateTime.now().minusHours(2)));

            assertThat(servicio().sincronizar(false)).isEqualTo(1);
            verify(cuotaService, never()).reservarConsulta(any(), anyInt());
            verifyNoInteractions(puntoRedClient);
        }

        @Test
        @DisplayName("Catálogo local vacío: consulta aunque la última sincronización sea reciente")
        void catalogoLocalVacio_ConsultaAunqueSeaReciente() {
            when(catalogoRepository.findByActivoTrue()).thenReturn(List.of());
            when(cuotaService.reservarConsulta(any(), anyInt())).thenReturn(true);
            when(puntoRedClient.getProductList()).thenReturn(new PuntoRedProductosResponse());

            servicio().sincronizar(false);

            verify(puntoRedClient).getProductList();
            verify(cuotaService, never()).ultimaConsultaExitosa(any());
        }

        @Test
        @DisplayName("Cupo diario agotado (scheduler): no llama al proveedor y conserva el catálogo")
        void cupoAgotado_NoLlama() {
            when(catalogoRepository.findByActivoTrue()).thenReturn(List.of(activo()));
            when(cuotaService.ultimaConsultaExitosa(any())).thenReturn(Optional.empty());
            when(cuotaService.reservarConsulta(any(), anyInt())).thenReturn(false);

            assertThat(servicio().sincronizar(false)).isEqualTo(1);
            verifyNoInteractions(puntoRedClient);
        }

        @Test
        @DisplayName("Forzada por ADMIN: ignora la antigüedad, pero con cupo agotado responde error sin llamar")
        void forzadaConCupoAgotado_Error() {
            when(cuotaService.reservarConsulta(any(), anyInt())).thenReturn(false);

            assertThatThrownBy(() -> servicio().sincronizar(true)).isInstanceOf(ReglaNegocioException.class);
            verify(cuotaService, never()).ultimaConsultaExitosa(any());
            verifyNoInteractions(puntoRedClient);
        }
    }
}
