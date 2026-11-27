package com.kapm.onboarding_clientes.service;

import com.kapm.onboarding_clientes.client.puntored.PuntoRedClient;
import com.kapm.onboarding_clientes.client.puntored.model.PuntoRedProducto;
import com.kapm.onboarding_clientes.client.puntored.model.PuntoRedProductosResponse;
import com.kapm.onboarding_clientes.config.PuntoRedProperties;
import com.kapm.onboarding_clientes.dto.request.ResolverConciliacionRequest;
import com.kapm.onboarding_clientes.dto.response.PagoResponse;
import com.kapm.onboarding_clientes.entity.ConsultaProveedorDiaria;
import com.kapm.onboarding_clientes.entity.EstatusPago;
import com.kapm.onboarding_clientes.entity.PagoServicio;
import com.kapm.onboarding_clientes.entity.catalogo.CatalogoProductoPuntoRed;
import com.kapm.onboarding_clientes.exception.PuntoRedIntegracionException;
import com.kapm.onboarding_clientes.exception.RecursoNoEncontradoException;
import com.kapm.onboarding_clientes.exception.ReglaNegocioException;
import com.kapm.onboarding_clientes.repository.CatalogoProductoPuntoRedRepository;
import com.kapm.onboarding_clientes.repository.ConsultaProveedorDiariaRepository;
import com.kapm.onboarding_clientes.repository.PagoServicioRepository;
import com.kapm.onboarding_clientes.scheduler.PuntoRedScheduler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("Servicios auxiliares: scheduler, cupo del proveedor, catálogo, conciliación y AES")
class ServiciosAuxiliaresTest {

    @Nested
    @ExtendWith(MockitoExtension.class)
    class Scheduler {
        @Mock VerificacionPagosService verificacionPagosService;
        @Mock CatalogoPuntoRedSyncService catalogoSyncService;

        @Test
        void verificaPendientesConYSinResueltos() {
            PuntoRedScheduler scheduler = new PuntoRedScheduler(verificacionPagosService, catalogoSyncService);
            when(verificacionPagosService.verificarPendientes()).thenReturn(0, 2);
            scheduler.verificarPagosSinConfirmar();
            scheduler.verificarPagosSinConfirmar();
            verify(verificacionPagosService, times(2)).verificarPendientes();
        }

        @Test
        void sincronizacionNuncaPropagaErrores() {
            PuntoRedScheduler scheduler = new PuntoRedScheduler(verificacionPagosService, catalogoSyncService);
            when(catalogoSyncService.sincronizar(false)).thenReturn(5).thenThrow(new IllegalStateException("caído"));
            scheduler.sincronizarCatalogoAlIniciar();
            scheduler.sincronizarCatalogo();
            verify(catalogoSyncService, times(2)).sincronizar(false);
        }
    }

    @Nested
    @ExtendWith(MockitoExtension.class)
    class CuotaProveedor {
        @Mock ConsultaProveedorDiariaRepository repository;

        @Test
        @DisplayName("Con zona horaria inválida usa America/Mexico_City y registra el éxito si hay fila del día")
        void zonaInvalidaYRegistroExito() {
            PuntoRedProperties props = new PuntoRedProperties();
            props.setZonaHoraria("Zona/Inexistente");
            CuotaConsultaProveedorServiceImpl servicio = new CuotaConsultaProveedorServiceImpl(repository, props);
            ConsultaProveedorDiaria registro = ConsultaProveedorDiaria.builder().operacion("op").consultas(1).build();
            when(repository.findByOperacionAndFechaWithLock(eq("op"), any())).thenReturn(Optional.of(registro));

            servicio.registrarExito("op");

            assertThat(registro.getUltimaExitosa()).isNotNull();
            verify(repository).save(registro);
        }

        @Test
        @DisplayName("Sin fila del día no registra nada")
        void registroExitoSinFila() {
            CuotaConsultaProveedorServiceImpl servicio = new CuotaConsultaProveedorServiceImpl(repository, new PuntoRedProperties());
            when(repository.findByOperacionAndFechaWithLock(eq("op"), any())).thenReturn(Optional.empty());
            servicio.registrarExito("op");
            verify(repository, never()).save(any());
        }
    }

    @Nested
    @ExtendWith(MockitoExtension.class)
    class SincronizacionCatalogo {
        @Mock PuntoRedClient puntoRedClient;
        @Mock CatalogoProductoPuntoRedRepository catalogoRepository;
        @Mock CuotaConsultaProveedorService cuotaService;

        private CatalogoPuntoRedSyncServiceImpl servicio() {
            return new CatalogoPuntoRedSyncServiceImpl(puntoRedClient, catalogoRepository, cuotaService, new PuntoRedProperties());
        }

        private static CatalogoProductoPuntoRed local(int id, boolean activo) {
            return CatalogoProductoPuntoRed.builder().idProducto(id).idServicio(1).tipoFront(0).activo(activo).build();
        }

        @Test
        @DisplayName("Con catálogo local y sincronización antigua sí consulta al proveedor")
        void sincronizacionAntiguaConsulta() {
            when(catalogoRepository.findByActivoTrue()).thenReturn(List.of(local(1, true)));
            when(cuotaService.ultimaConsultaExitosa(anyString())).thenReturn(Optional.of(LocalDateTime.now().minusDays(2)));
            when(cuotaService.reservarConsulta(anyString(), anyInt())).thenReturn(true);
            when(puntoRedClient.getProductList()).thenReturn(null);

            assertThat(servicio().sincronizar(false)).isEqualTo(1);
            verify(puntoRedClient).getProductList();
        }

        @Test
        @DisplayName("Con catálogo local y sin sincronización previa sí consulta al proveedor")
        void sinSincronizacionPrevia() {
            when(catalogoRepository.findByActivoTrue()).thenReturn(List.of(local(1, true)));
            when(cuotaService.ultimaConsultaExitosa(anyString())).thenReturn(Optional.empty());
            when(cuotaService.reservarConsulta(anyString(), anyInt())).thenReturn(false);

            assertThat(servicio().sincronizar(false)).isEqualTo(1);
            verify(puntoRedClient, never()).getProductList();
        }

        @Test
        @DisplayName("Respuestas sin productos conservan el catálogo local")
        void respuestasVacias() {
            when(catalogoRepository.findByActivoTrue()).thenReturn(List.of());
            when(cuotaService.reservarConsulta(anyString(), anyInt())).thenReturn(true);
            PuntoRedProductosResponse sinLista = new PuntoRedProductosResponse();
            sinLista.setProductos(null);
            when(puntoRedClient.getProductList()).thenReturn(sinLista, new PuntoRedProductosResponse());

            assertThat(servicio().sincronizar(true)).isZero();
            assertThat(servicio().sincronizar(false)).isZero();
            verify(catalogoRepository, never()).save(any());
        }

        @Test
        @DisplayName("Descarta productos incompletos, toma el primero de los duplicados y conserva inactivos ausentes")
        void filtraProductos() {
            List<PuntoRedProducto> remotos = new ArrayList<>(List.of(
                    PuntoRedProducto.builder().idProducto(1).idServicio(1).tipoFront(0).producto("A").servicio("S").build(),
                    PuntoRedProducto.builder().idProducto(1).idServicio(9).tipoFront(0).build(),
                    PuntoRedProducto.builder().idProducto(null).idServicio(1).tipoFront(0).build(),
                    PuntoRedProducto.builder().idProducto(2).idServicio(null).tipoFront(0).build(),
                    PuntoRedProducto.builder().idProducto(3).idServicio(1).tipoFront(null).build()));
            when(catalogoRepository.findByActivoTrue()).thenReturn(List.of());
            when(cuotaService.reservarConsulta(anyString(), anyInt())).thenReturn(true);
            when(puntoRedClient.getProductList()).thenReturn(PuntoRedProductosResponse.builder().productos(remotos).build());
            CatalogoProductoPuntoRed inactivo = local(4, false);
            when(catalogoRepository.findAll()).thenReturn(List.of(inactivo));

            assertThat(servicio().sincronizar(true)).isEqualTo(1);
            verify(catalogoRepository, never()).save(inactivo);
        }

        @Test
        @DisplayName("Forzar con cupo agotado responde regla de negocio")
        void forzarSinCupo() {
            when(catalogoRepository.findByActivoTrue()).thenReturn(List.of());
            when(cuotaService.reservarConsulta(anyString(), anyInt())).thenReturn(false);
            assertThatThrownBy(() -> servicio().sincronizar(true)).isInstanceOf(ReglaNegocioException.class);
        }
    }

    @Nested
    @ExtendWith(MockitoExtension.class)
    class Conciliacion {
        @Mock PagoServicioRepository pagoServicioRepository;
        @Mock PagoTransaccionalService pagoTransaccionalService;

        @Test
        @DisplayName("Lista los pagos en conciliación")
        void listar() {
            PagoServicio pago = PagoServicio.builder().id(1L).upc("UPC1").numeroCuenta("1234567890")
                    .monto(new BigDecimal("10.00")).estatus(EstatusPago.EN_CONCILIACION).build();
            when(pagoServicioRepository.findByEstatus(EstatusPago.EN_CONCILIACION)).thenReturn(List.of(pago));

            List<PagoResponse> pagos = new ConciliacionPagosServiceImpl(pagoServicioRepository, pagoTransaccionalService).listarEnConciliacion();

            assertThat(pagos).hasSize(1);
            assertThat(pagos.get(0).getUpc()).isEqualTo("UPC1");
        }

        @Test
        @DisplayName("Resolver un UPC inexistente responde 404")
        void resolverInexistente() {
            when(pagoServicioRepository.findByUpc("NOPE")).thenReturn(Optional.empty());
            ConciliacionPagosServiceImpl servicio = new ConciliacionPagosServiceImpl(pagoServicioRepository, pagoTransaccionalService);
            ResolverConciliacionRequest request = ResolverConciliacionRequest.builder().upc(" NOPE ").procesadaPorProveedor(false).build();
            assertThatThrownBy(() -> servicio.resolver(request))
                    .isInstanceOf(RecursoNoEncontradoException.class);
        }
    }

    @Nested
    class Aes {
        private static final String KEY = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";
        private static final String IV = "MDEyMzQ1Njc4OWFiY2RlZg==";

        @Test
        @DisplayName("Sin inicializar, cifrar falla de forma controlada")
        void sinInicializar() {
            AesEncryptionService servicio = new AesEncryptionService(new PuntoRedProperties());
            assertThatThrownBy(() -> servicio.encrypt("hola"))
                    .isInstanceOf(PuntoRedIntegracionException.class)
                    .hasMessageContaining("cifrado AES");
        }

        @Test
        @DisplayName("Valida nulos y llaves ausentes con parámetros explícitos")
        void parametrosExplicitos() {
            AesEncryptionService servicio = new AesEncryptionService(new PuntoRedProperties());
            assertThatThrownBy(() -> servicio.decrypt(null, KEY, IV)).isInstanceOf(PuntoRedIntegracionException.class);
            assertThatThrownBy(() -> servicio.encrypt(null, KEY, IV)).isInstanceOf(PuntoRedIntegracionException.class);
            assertThatThrownBy(() -> servicio.construirParametros(null, IV)).hasMessageContaining("llave AES");
            assertThatThrownBy(() -> servicio.construirParametros(" ", IV)).hasMessageContaining("llave AES");
            assertThatThrownBy(() -> servicio.construirParametros(KEY, null)).hasMessageContaining("IV");
            assertThatThrownBy(() -> servicio.construirParametros(KEY, " ")).hasMessageContaining("IV");
            String cifrado = servicio.encrypt("hola", KEY, IV);
            assertThat(servicio.decrypt(cifrado, KEY, IV)).isEqualTo("hola");
        }
    }
}
