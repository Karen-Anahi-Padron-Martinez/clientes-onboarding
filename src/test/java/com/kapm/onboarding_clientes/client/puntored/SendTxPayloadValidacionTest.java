package com.kapm.onboarding_clientes.client.puntored;

import com.kapm.onboarding_clientes.client.puntored.model.PuntoRedMensaje;
import com.kapm.onboarding_clientes.client.puntored.model.PuntoRedTxResponse;
import com.kapm.onboarding_clientes.client.puntored.model.SendTxPayload;
import com.kapm.onboarding_clientes.exception.PuntoRedIntegracionException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@DisplayName("SendTxPayload: cada regla de validación y modelos de respuesta")
class SendTxPayloadValidacionTest {

    private static SendTxPayload base() {
        return SendTxPayload.builder()
                .idProducto(1)
                .idServicio(2)
                .telefono("5512345678")
                .horaLocal("20261023190000")
                .upc("UPC123")
                .unidad("SUCURSAL")
                .build();
    }

    private static Arguments caso(String campo, Consumer<SendTxPayload> cambio) {
        return arguments(campo, cambio);
    }

    static Stream<Arguments> invalidos() {
        String largo101 = "R".repeat(101);
        return Stream.of(
                caso("idProducto", p -> p.setIdProducto(0)),
                caso("idServicio", p -> p.setIdServicio(0)),
                caso("telefono", p -> p.setTelefono(null)),
                caso("telefono", p -> p.setTelefono("123")),
                caso("telefono", p -> p.setTelefono("1".repeat(21))),
                caso("horaLocal", p -> p.setHoraLocal(null)),
                caso("horaLocal", p -> p.setHoraLocal("2026-10-23")),
                caso("upc", p -> p.setUpc(null)),
                caso("upc", p -> p.setUpc("AB")),
                caso("upc", p -> p.setUpc("U".repeat(16))),
                caso("unidad", p -> p.setUnidad(null)),
                caso("unidad", p -> p.setUnidad("AB")),
                caso("unidad", p -> p.setUnidad("U".repeat(26))),
                // Pago de servicio
                caso("referencia", p -> p.setTipoFront(1)),
                caso("referencia", p -> { p.setTipoFront(2); p.setReferencia("   "); }),
                caso("referencia", p -> { p.setTipoFront(1); p.setReferencia(largo101); }),
                caso("montoPago", p -> { p.setTipoFront(1); p.setReferencia("REF"); }),
                caso("montoPago", p -> { p.setReferencia("REF"); p.setMontoPago(BigDecimal.ZERO); }),
                caso("2 decimales", p -> { p.setReferencia("REF"); p.setMontoPago(new BigDecimal("1.001")); }),
                // Recarga
                caso("recargas", p -> p.setTipoFront(0)),
                caso("recargas", p -> { p.setTipoFront(0); p.setMontoPago(BigDecimal.ZERO); }),
                caso("2 decimales", p -> { p.setTipoFront(0); p.setMontoPago(new BigDecimal("10.555")); })
        );
    }

    @ParameterizedTest(name = "[{index}] rechaza {0}")
    @MethodSource("invalidos")
    void rechazaCamposInvalidos(String campo, Consumer<SendTxPayload> cambio) {
        SendTxPayload payload = base();
        cambio.accept(payload);
        assertThatThrownBy(payload::validar)
                .isInstanceOf(PuntoRedIntegracionException.class)
                .hasMessageContaining(campo);
    }

    static Stream<Arguments> validos() {
        return Stream.of(
                caso("sin tipoFront ni referencia", p -> { }),
                caso("sin tipoFront con referencia en blanco", p -> p.setReferencia(" ")),
                caso("tipoFront desconocido", p -> p.setTipoFront(5)),
                caso("recarga", p -> { p.setTipoFront(0); p.setMontoPago(new BigDecimal("50.00")); p.setReferencia("ignorada"); }),
                caso("servicio por referencia", p -> { p.setReferencia("REF"); p.setMontoPago(new BigDecimal("10.5")); })
        );
    }

    @ParameterizedTest(name = "[{index}] acepta {0}")
    @MethodSource("validos")
    void aceptaPayloadsValidos(String descripcion, Consumer<SendTxPayload> cambio) {
        SendTxPayload payload = base();
        cambio.accept(payload);
        assertThatCode(payload::validar).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("El teléfono enmascarado nunca expone más de 4 dígitos")
    void telefonoEnmascarado() {
        SendTxPayload payload = base();
        assertThat(payload.getTelefonoEnmascarado()).isEqualTo("******5678");
        payload.setTelefono("123");
        assertThat(payload.getTelefonoEnmascarado()).isEqualTo("******");
        payload.setTelefono(null);
        assertThat(payload.getTelefonoEnmascarado()).isEqualTo("******");
    }

    @Test
    @DisplayName("PuntoRedTxResponse parsea la fecha en inglés y tolera ausencia")
    void fechaRespuesta() {
        PuntoRedTxResponse respuesta = new PuntoRedTxResponse();
        assertThat(respuesta.getFechaParsed()).isNull();
        respuesta.setFecha(" ");
        assertThat(respuesta.getFechaParsed()).isNull();
        respuesta.setFecha("25/Feb/2022 18:15:25");
        assertThat(respuesta.getFechaParsed()).isEqualTo(LocalDateTime.of(2022, 2, 25, 18, 15, 25));
    }

    @Test
    @DisplayName("PuntoRedMensaje solo es exitoso con CODIGO 01")
    void mensajeExitoso() {
        assertThat(new PuntoRedMensaje("01", "OK").isExitoso()).isTrue();
        assertThat(new PuntoRedMensaje("06", "Rechazo").isExitoso()).isFalse();
        assertThat(PuntoRedMensaje.builder().codigo("01").idTx("TX").build().getIdTx()).isEqualTo("TX");
    }
}
