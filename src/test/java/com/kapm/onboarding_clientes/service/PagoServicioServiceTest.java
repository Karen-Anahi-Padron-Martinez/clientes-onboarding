package com.kapm.onboarding_clientes.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kapm.onboarding_clientes.client.puntored.model.SendTxPayload;
import com.kapm.onboarding_clientes.config.PuntoRedProperties;
import com.kapm.onboarding_clientes.entity.EstatusPago;
import com.kapm.onboarding_clientes.entity.PagoServicio;
import com.kapm.onboarding_clientes.entity.catalogo.CatalogoProductoPuntoRed;
import com.kapm.onboarding_clientes.exception.PuntoRedIntegracionException;
import com.kapm.onboarding_clientes.exception.ReglaNegocioException;
import com.kapm.onboarding_clientes.repository.CatalogoProductoPuntoRedRepository;
import com.kapm.onboarding_clientes.repository.PagoServicioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Pruebas Unitarias - PagoServicioService (reconstrucción de SendTxPayload para verificación)")
class PagoServicioServiceTest {

    private static final String UPC_ORIGINAL = "LS32K918A4F7Z";

    @Mock
    private PagoServicioRepository pagoServicioRepository;

    @Mock
    private CatalogoProductoPuntoRedRepository catalogoProductoRepository;

    @Spy
    private PuntoRedProperties puntoRedProperties = new PuntoRedProperties();

    @InjectMocks
    private PagoServicioServiceImpl pagoServicioService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        puntoRedProperties.setUnidad("SUCURSAL_CENTRO");
        puntoRedProperties.setZonaHoraria("America/Mexico_City");
    }

    private PagoServicio pagoServicioCfe() {
        return PagoServicio.builder()
                .id(10L)
                .upc(UPC_ORIGINAL)
                .curp("MEHC920824HDFRMN09")
                .rfc("MEHC9208243A8")
                .numeroCuenta("4819204812")
                .idempotencyKey("KEY-1")
                .idProducto(55)
                .idServicio(2)
                .telefono("5512345678")
                .referencia("0123456789012345")
                .monto(new BigDecimal("350.50"))
                .horaLocal("20260101000000")
                .unidad("SUCURSAL_CENTRO")
                .estatus(EstatusPago.PENDIENTE)
                .build();
    }

    @Test
    @DisplayName("Reutiliza EXACTAMENTE el mismo UPC y los datos originales del pago")
    void reutilizaMismoUpc() {
        when(catalogoProductoRepository.findByIdProducto(55)).thenReturn(Optional.of(
                CatalogoProductoPuntoRed.builder().idProducto(55).tipoFront(1).build()));

        SendTxPayload payload = pagoServicioService.armarPayloadDesdePago(pagoServicioCfe());

        assertThat(payload.getUpc()).isEqualTo(UPC_ORIGINAL);
        assertThat(payload.getIdProducto()).isEqualTo(55);
        assertThat(payload.getIdServicio()).isEqualTo(2);
        assertThat(payload.getReferencia()).isEqualTo("0123456789012345");
        assertThat(payload.getMontoPago()).isEqualByComparingTo("350.50");
        assertThat(payload.getUnidad()).isEqualTo("SUCURSAL_CENTRO");
        // La hora local se renueva en cada envío; la deduplicación del proveedor es por UPC
        assertThat(payload.getHoraLocal()).matches("^\\d{14}$").isNotEqualTo("20260101000000");
    }

    @Test
    @DisplayName("Cero fuga de datos: CURP, RFC, cuenta, llave de idempotencia y tipoFront nunca viajan a PuntoRed")
    void sinDatosInternosEnLaTrama() throws Exception {
        when(catalogoProductoRepository.findByIdProducto(55)).thenReturn(Optional.of(
                CatalogoProductoPuntoRed.builder().idProducto(55).tipoFront(1).build()));

        SendTxPayload payload = pagoServicioService.armarPayloadDesdePago(pagoServicioCfe());

        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(payload));
        assertThat(json.has("curp")).isFalse();
        assertThat(json.has("rfc")).isFalse();
        assertThat(json.has("numeroCuenta")).isFalse();
        assertThat(json.has("idempotencyKey")).isFalse();
        assertThat(json.has("tipoFront")).isFalse();
        // El toString (que podría terminar en un log) tampoco expone teléfono ni referencia
        assertThat(payload.toString()).doesNotContain("5512345678").doesNotContain("0123456789012345");
    }

    @Test
    @DisplayName("Recarga (tipoFront 0): no requiere referencia")
    void recargaSinReferencia() {
        PagoServicio recarga = pagoServicioCfe();
        recarga.setIdProducto(20);
        recarga.setReferencia(null);
        recarga.setMonto(new BigDecimal("100.00"));
        when(catalogoProductoRepository.findByIdProducto(20)).thenReturn(Optional.of(
                CatalogoProductoPuntoRed.builder().idProducto(20).tipoFront(0).build()));

        SendTxPayload payload = pagoServicioService.armarPayloadDesdePago(recarga);

        assertThat(payload.getReferencia()).isNull();
        assertThat(payload.getUpc()).isEqualTo(UPC_ORIGINAL);
    }

    @Test
    @DisplayName("Producto retirado del catálogo: se reconstruye igual infiriendo el tipo por la referencia")
    void productoRetiradoDelCatalogo() {
        when(catalogoProductoRepository.findByIdProducto(55)).thenReturn(Optional.empty());

        SendTxPayload payload = pagoServicioService.armarPayloadDesdePago(pagoServicioCfe());

        assertThat(payload.getTipoFront()).isNull();
        assertThat(payload.getUpc()).isEqualTo(UPC_ORIGINAL);
    }

    @Test
    @DisplayName("Servicio sin referencia en BD: la trama es inválida y no se envía")
    void servicioSinReferencia_Invalido() {
        PagoServicio pago = pagoServicioCfe();
        pago.setReferencia(null);
        when(catalogoProductoRepository.findByIdProducto(55)).thenReturn(Optional.of(
                CatalogoProductoPuntoRed.builder().idProducto(55).tipoFront(1).build()));

        assertThatThrownBy(() -> pagoServicioService.armarPayloadDesdePago(pago))
                .isInstanceOf(PuntoRedIntegracionException.class)
                .hasMessageContaining("referencia");
    }

    @Test
    @DisplayName("Pago nulo: regla de negocio")
    void pagoNulo() {
        assertThatThrownBy(() -> pagoServicioService.armarPayloadDesdePago(null))
                .isInstanceOf(ReglaNegocioException.class);
    }
}
