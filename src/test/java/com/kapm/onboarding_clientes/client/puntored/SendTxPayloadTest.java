package com.kapm.onboarding_clientes.client.puntored;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kapm.onboarding_clientes.client.puntored.model.SendTxPayload;
import com.kapm.onboarding_clientes.exception.PuntoRedIntegracionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Pruebas Unitarias - SendTxPayload (Estructura y Validación)")
class SendTxPayloadTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("Serialización JSON incluye SOLO los 8 campos oficiales y excluye tipoFront")
    void testSerializacionJsonSoloCamposOficiales() throws Exception {
        SendTxPayload payload = SendTxPayload.builder()
                .idProducto(55)
                .idServicio(2)
                .telefono("5512345678")
                .horaLocal("20261023190000")
                .referencia("012345678901")
                .montoPago(new BigDecimal("150.00"))
                .upc("UPC12345678901")
                .unidad("SUCURSAL_CENTRO")
                .tipoFront(1) // Debe ser ignorado por @JsonIgnore
                .build();

        payload.validar();
        String json = objectMapper.writeValueAsString(payload);
        JsonNode node = objectMapper.readTree(json);

        // Los 8 campos oficiales requeridos por PuntoRed
        assertThat(node.has("idProducto")).isTrue();
        assertThat(node.has("idServicio")).isTrue();
        assertThat(node.has("telefono")).isTrue();
        assertThat(node.has("horaLocal")).isTrue();
        assertThat(node.has("referencia")).isTrue();
        assertThat(node.has("montoPago")).isTrue();
        assertThat(node.has("upc")).isTrue();
        assertThat(node.has("unidad")).isTrue();

        // NO debe contener tipoFront ni identificadores bancarios internos
        assertThat(node.has("tipoFront")).isFalse();
        assertThat(node.has("curp")).isFalse();
        assertThat(node.has("rfc")).isFalse();
        assertThat(node.has("numeroCuenta")).isFalse();
        assertThat(node.has("idUser")).isFalse();
    }

    @Test
    @DisplayName("Validación exitosa para pago de servicio (tipoFront = 1) con referencia y monto")
    void testValidacionExitosaPagoServicio() {
        SendTxPayload payload = SendTxPayload.builder()
                .idProducto(55)
                .idServicio(2)
                .telefono("5512345678")
                .horaLocal("20261023190000")
                .referencia("RECIBO-CFE-001")
                .montoPago(new BigDecimal("230.50"))
                .upc("LS32K918A4F7Z")
                .unidad("SUCURSAL_MATRIZ")
                .tipoFront(1)
                .build();

        payload.validar();
        assertThat(payload.getTelefonoEnmascarado()).isEqualTo("******5678");
    }

    @Test
    @DisplayName("Validación exitosa para recarga TAE (tipoFront = 0) sin referencia")
    void testValidacionExitosaRecargaSinReferencia() {
        SendTxPayload payload = SendTxPayload.builder()
                .idProducto(20)
                .idServicio(1)
                .telefono("5598765432")
                .horaLocal("20261023190000")
                .referencia(null) // En recargas no se requiere referencia
                .montoPago(new BigDecimal("100.00"))
                .upc("LS32K918A4F7Z")
                .unidad("SUCURSAL_MATRIZ")
                .tipoFront(0)
                .build();

        payload.validar();
    }

    @Test
    @DisplayName("Pago de servicio sin referencia lanza excepción")
    void testPagoServicioSinReferenciaLanzaExcepcion() {
        SendTxPayload payload = SendTxPayload.builder()
                .idProducto(55)
                .idServicio(2)
                .telefono("5512345678")
                .horaLocal("20261023190000")
                .referencia(null)
                .montoPago(new BigDecimal("200.00"))
                .upc("LS32K918A4F7Z")
                .unidad("SUCURSAL_MATRIZ")
                .tipoFront(1)
                .build();

        assertThatThrownBy(payload::validar)
                .isInstanceOf(PuntoRedIntegracionException.class)
                .hasMessageContaining("referencia");
    }

    @ParameterizedTest
    @ValueSource(strings = {"2026", "20261023", "2026102319000", "202610231900000", "2026102319ABCD"})
    @DisplayName("horaLocal con formato distinto de yyyyMMddHHmmss (14 dígitos) lanza excepción")
    void testHoraLocalInvalida(String horaInvalida) {
        SendTxPayload payload = SendTxPayload.builder()
                .idProducto(55)
                .idServicio(2)
                .telefono("5512345678")
                .horaLocal(horaInvalida)
                .referencia("REF123")
                .montoPago(new BigDecimal("100.00"))
                .upc("LS32K918A4F7Z")
                .unidad("SUCURSAL_MATRIZ")
                .tipoFront(1)
                .build();

        assertThatThrownBy(payload::validar)
                .isInstanceOf(PuntoRedIntegracionException.class)
                .hasMessageContaining("horaLocal");
    }

    @Test
    @DisplayName("Monto con más de 2 decimales lanza excepción")
    void testMontoConMasDeDosDecimalesLanzaExcepcion() {
        SendTxPayload payload = SendTxPayload.builder()
                .idProducto(55)
                .idServicio(2)
                .telefono("5512345678")
                .horaLocal("20261023190000")
                .referencia("REF123")
                .montoPago(new BigDecimal("100.555"))
                .upc("LS32K918A4F7Z")
                .unidad("SUCURSAL_MATRIZ")
                .tipoFront(1)
                .build();

        assertThatThrownBy(payload::validar)
                .isInstanceOf(PuntoRedIntegracionException.class)
                .hasMessageContaining("decimales");
    }

    @Test
    @DisplayName("UPC mayor a 15 caracteres lanza excepción")
    void testUpcMayorA15LanzaExcepcion() {
        SendTxPayload payload = SendTxPayload.builder()
                .idProducto(55)
                .idServicio(2)
                .telefono("5512345678")
                .horaLocal("20261023190000")
                .referencia("REF123")
                .montoPago(new BigDecimal("100.00"))
                .upc("1234567890123456") // 16 caracteres
                .unidad("SUCURSAL_MATRIZ")
                .tipoFront(1)
                .build();

        assertThatThrownBy(payload::validar)
                .isInstanceOf(PuntoRedIntegracionException.class)
                .hasMessageContaining("upc");
    }
}
