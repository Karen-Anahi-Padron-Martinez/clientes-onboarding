package com.kapm.onboarding_clientes.controller;

import com.kapm.onboarding_clientes.dto.request.*;
import com.kapm.onboarding_clientes.dto.response.*;
import com.kapm.onboarding_clientes.service.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("Controladores: delegan en el servicio y envuelven la respuesta en ApiResponse")
class ControladoresTest {

    private static <T> T datosOk(ResponseEntity<ApiResponse<T>> respuesta) {
        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).isNotNull();
        assertThat(respuesta.getBody().isExito()).isTrue();
        return respuesta.getBody().getDatos();
    }

    @Test
    @DisplayName("ApiResponse.error marca la respuesta como fallida sin datos")
    void apiResponseError() {
        ApiResponse<String> error = ApiResponse.error("falló");
        assertThat(error.isExito()).isFalse();
        assertThat(error.getMensaje()).isEqualTo("falló");
        assertThat(error.getDatos()).isNull();
        assertThat(error.getTimestamp()).isNotNull();
    }

    @Nested
    @ExtendWith(MockitoExtension.class)
    class Catalogos {
        @Mock CatalogoService catalogoService;
        @Mock CodigoPostalService codigoPostalService;
        @InjectMocks CatalogoController controller;

        @Test
        void todosLosEndpoints() {
            List<CatalogoItemDTO> items = List.of(new CatalogoItemDTO("A", "a"));
            CatalogosResponse todos = CatalogosResponse.builder().build();
            List<String> municipios = List.of("Cuauhtémoc");
            when(catalogoService.obtenerTodosLosCatalogos()).thenReturn(todos);
            when(catalogoService.obtenerSexos()).thenReturn(items);
            when(catalogoService.obtenerEstadosCiviles()).thenReturn(items);
            when(catalogoService.obtenerNacionalidades()).thenReturn(items);
            when(catalogoService.obtenerOcupaciones()).thenReturn(items);
            when(catalogoService.obtenerEstadosRepublica()).thenReturn(items);
            when(catalogoService.obtenerPaises()).thenReturn(items);
            when(catalogoService.obtenerTiposBiometria()).thenReturn(items);
            when(catalogoService.obtenerEstatusCuenta()).thenReturn(items);
            when(codigoPostalService.municipiosPorEstado("CIUDAD_DE_MEXICO")).thenReturn(municipios);

            assertThat(datosOk(controller.obtenerTodosLosCatalogos())).isSameAs(todos);
            assertThat(datosOk(controller.obtenerSexos())).isSameAs(items);
            assertThat(datosOk(controller.obtenerEstadosCiviles())).isSameAs(items);
            assertThat(datosOk(controller.obtenerNacionalidades())).isSameAs(items);
            assertThat(datosOk(controller.obtenerOcupaciones())).isSameAs(items);
            assertThat(datosOk(controller.obtenerEstadosRepublica())).isSameAs(items);
            assertThat(datosOk(controller.obtenerPaises())).isSameAs(items);
            assertThat(datosOk(controller.obtenerTiposBiometria())).isSameAs(items);
            assertThat(datosOk(controller.obtenerEstatusCuenta())).isSameAs(items);
            assertThat(datosOk(controller.municipios("CIUDAD_DE_MEXICO"))).isSameAs(municipios);
        }
    }

    @Nested
    @ExtendWith(MockitoExtension.class)
    class Cuentas {
        @Mock CuentaService cuentaService;
        @InjectMocks CuentaController controller;

        @Test
        void todosLosEndpoints() {
            List<CuentaResponse> cuentas = List.of(new CuentaResponse());
            CuentaResponse cuenta = new CuentaResponse();
            SaldoResponse saldo = new SaldoResponse();
            CuentaConsultaRequest request = new CuentaConsultaRequest("1234567890");
            when(cuentaService.obtenerCuentasActivas()).thenReturn(cuentas);
            when(cuentaService.obtenerCuentaPorNumero("1234567890")).thenReturn(cuenta);
            when(cuentaService.obtenerSaldoCuenta("1234567890")).thenReturn(saldo);

            assertThat(datosOk(controller.obtenerCuentasActivas())).isSameAs(cuentas);
            assertThat(datosOk(controller.obtenerCuentasActivasPost())).isSameAs(cuentas);
            assertThat(datosOk(controller.obtenerCuentaPorNumero(request))).isSameAs(cuenta);
            assertThat(datosOk(controller.obtenerSaldoCuenta(request))).isSameAs(saldo);
        }
    }

    @Nested
    @ExtendWith(MockitoExtension.class)
    class Conciliacion {
        @Mock ConciliacionPagosService conciliacionPagosService;
        @Mock CatalogoPuntoRedSyncService catalogoSyncService;
        @Mock VerificacionPagosService verificacionPagosService;
        @InjectMocks ConciliacionController controller;

        @Test
        void todosLosEndpoints() {
            PagoResponse pago = PagoResponse.builder().mensaje("ok").build();
            List<PagoResponse> pagos = List.of(pago);
            ResolverConciliacionRequest resolver = ResolverConciliacionRequest.builder().upc("UPC").procesadaPorProveedor(false).build();
            when(conciliacionPagosService.listarEnConciliacion()).thenReturn(pagos);
            when(verificacionPagosService.verificar("UPC")).thenReturn(pago);
            when(conciliacionPagosService.resolver(resolver)).thenReturn(pago);
            when(catalogoSyncService.sincronizar(true)).thenReturn(12);

            assertThat(datosOk(controller.listarEnConciliacion())).isSameAs(pagos);
            assertThat(datosOk(controller.verificar(new VerificarPagoRequest("UPC")))).isSameAs(pago);
            assertThat(datosOk(controller.conciliar(resolver))).isSameAs(pago);
            assertThat(datosOk(controller.sincronizarCatalogo())).isEqualTo(12);
        }
    }

    @Nested
    @ExtendWith(MockitoExtension.class)
    class Clientes {
        @Mock ClienteService clienteService;
        @InjectMocks ClienteController controller;

        @Test
        void todosLosEndpoints() {
            ClienteResponse cliente = new ClienteResponse();
            ClienteRegistrationRequest registro = new ClienteRegistrationRequest();
            ActualizarPorCurpRequest porCurp = ActualizarPorCurpRequest.builder().curp("CURP").build();
            ActualizarPorRfcRequest porRfc = ActualizarPorRfcRequest.builder().rfc("RFC").build();
            ClienteUpdateRequest update = new ClienteUpdateRequest();
            BuscarClienteRequest buscar = new BuscarClienteRequest();
            ClienteIdentificadorRequest id = new ClienteIdentificadorRequest("CURP", null);
            ValidarIdentificadorRequest validar = new ValidarIdentificadorRequest("CURP", null);
            ActualizarContactoRequest contacto = new ActualizarContactoRequest();
            PageResponse<ClienteResponse> pagina = new PageResponse<>();
            ValidacionIdentificadorResponse validacion = ValidacionIdentificadorResponse.builder().mensaje("Disponible").build();
            CuentaResponse cuenta = new CuentaResponse();
            List<ClienteResponse> lista = List.of(cliente);

            when(clienteService.registrarCliente(registro)).thenReturn(cliente);
            when(clienteService.actualizarClientePorCurp(porCurp)).thenReturn(cliente);
            when(clienteService.actualizarClientePorRfc(porRfc)).thenReturn(cliente);
            when(clienteService.actualizarCliente(update)).thenReturn(cliente);
            when(clienteService.buscarClientes(buscar)).thenReturn(pagina);
            when(clienteService.reactivarCliente(id)).thenReturn(cliente);
            when(clienteService.validarDisponibilidad(validar)).thenReturn(validacion);
            when(clienteService.actualizarContacto(contacto)).thenReturn(cliente);
            when(clienteService.obtenerCuenta(id)).thenReturn(cuenta);
            when(clienteService.obtenerTodosLosClientes()).thenReturn(lista);
            when(clienteService.obtenerClientesActivos()).thenReturn(lista);

            ResponseEntity<ApiResponse<ClienteResponse>> creado = controller.registrarCliente(registro);
            assertThat(creado.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(creado.getBody().getDatos()).isSameAs(cliente);

            assertThat(controller.actualizarClientePorCurp(porCurp).getBody().getMensaje()).contains("CURP");
            assertThat(controller.actualizarClientePorRfc(porRfc).getBody().getMensaje()).contains("RFC");
            assertThat(datosOk(controller.actualizarCliente(update))).isSameAs(cliente);
            assertThat(datosOk(controller.buscarCliente(buscar))).isSameAs(pagina);
            assertThat(datosOk(controller.desactivarCliente(id))).isNull();
            assertThat(datosOk(controller.desactivarClienteDelete(id))).isNull();
            verify(clienteService, times(2)).desactivarCliente(id);
            assertThat(datosOk(controller.reactivarClientePost(id))).isSameAs(cliente);
            assertThat(datosOk(controller.reactivarClientePatch(id))).isSameAs(cliente);
            assertThat(controller.validarDisponibilidad(validar).getBody().getMensaje()).isEqualTo("Disponible");
            assertThat(datosOk(controller.actualizarContacto(contacto))).isSameAs(cliente);
            assertThat(datosOk(controller.obtenerCuenta(id))).isSameAs(cuenta);
            assertThat(datosOk(controller.obtenerTodosLosClientes())).isSameAs(lista);
            assertThat(datosOk(controller.obtenerClientesActivos())).isSameAs(lista);
        }
    }
}
