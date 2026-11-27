package com.kapm.onboarding_clientes.client.brevo;

import com.kapm.onboarding_clientes.config.BrevoProperties;
import com.kapm.onboarding_clientes.exception.CorreoNoEnviadoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.mock.http.client.MockClientHttpResponse;
import org.springframework.web.client.RestClient;

import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@DisplayName("BrevoEmailClient (unitarias): validaciones, respuestas y fallas de red")
class BrevoEmailClientUnitTest {

    private static final String DESTINO = "cliente@correo.com";

    private BrevoProperties properties;

    @BeforeEach
    void setUp() {
        properties = new BrevoProperties();
        properties.setApiKey("xkeysib-key");
        properties.setSenderEmail("remitente@banco.com");
    }

    /** Fábrica que responde siempre con el estatus y cuerpo indicados, sin red. */
    private static ClientHttpRequestFactory responde(HttpStatus status, String cuerpo) {
        return (uri, method) -> {
            MockClientHttpRequest request = new MockClientHttpRequest(method, uri);
            MockClientHttpResponse response = new MockClientHttpResponse(cuerpo.getBytes(StandardCharsets.UTF_8), status);
            response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
            request.setResponse(response);
            return request;
        };
    }

    private BrevoEmailClient cliente(ClientHttpRequestFactory factory) {
        return new BrevoEmailClient(RestClient.builder().requestFactory(factory).build(), properties, null);
    }

    private String enviar(ClientHttpRequestFactory factory) {
        return cliente(factory).enviarCorreo(DESTINO, "Asunto", "<p>hola</p>");
    }

    static Stream<Arguments> parametrosInvalidos() {
        return Stream.of(
                arguments(null, "A", "<p/>", "destinatario"),
                arguments(" ", "A", "<p/>", "destinatario"),
                arguments("sin-arroba", "A", "<p/>", "formato"),
                arguments("a@b", "A", "<p/>", "formato"),
                arguments(DESTINO, null, "<p/>", "asunto"),
                arguments(DESTINO, " ", "<p/>", "asunto"),
                arguments(DESTINO, "A", null, "HTML"),
                arguments(DESTINO, "A", " ", "HTML"));
    }

    @ParameterizedTest(name = "[{index}] rechaza {3}")
    @MethodSource("parametrosInvalidos")
    void rechazaParametros(String destino, String asunto, String html, String campo) {
        BrevoEmailClient client = cliente(responde(HttpStatus.CREATED, "{}"));
        assertThatThrownBy(() -> client.enviarCorreo(destino, asunto, html))
                .isInstanceOf(CorreoNoEnviadoException.class)
                .hasMessageContaining(campo);
    }

    @Test
    @DisplayName("Exige API key y remitente configurados")
    void configuracionIncompleta() {
        ClientHttpRequestFactory ok = responde(HttpStatus.CREATED, "{}");
        properties.setApiKey(null);
        assertThatThrownBy(() -> enviar(ok)).hasMessageContaining("API key");
        properties.setApiKey(" ");
        assertThatThrownBy(() -> enviar(ok)).hasMessageContaining("API key");
        properties.setApiKey("key");
        properties.setSenderEmail(null);
        assertThatThrownBy(() -> enviar(ok)).hasMessageContaining("remitente");
        properties.setSenderEmail(" ");
        assertThatThrownBy(() -> enviar(ok)).hasMessageContaining("remitente");
    }

    @Test
    @DisplayName("El messageId es opcional: cuerpo vacío, sin campo o ilegible devuelven null")
    void messageIdOpcional() {
        properties.setSenderName(null);
        assertThat(enviar(responde(HttpStatus.CREATED, "{\"messageId\":\"<id-1>\"}"))).isEqualTo("<id-1>");
        assertThat(enviar(responde(HttpStatus.CREATED, ""))).isNull();
        assertThat(enviar(responde(HttpStatus.CREATED, "   "))).isNull();
        assertThat(enviar(responde(HttpStatus.CREATED, "{\"otro\":1}"))).isNull();
        assertThat(enviar(responde(HttpStatus.CREATED, "no-es-json"))).isNull();
    }

    @Test
    @DisplayName("HTTP 401 y otros errores HTTP se traducen a CorreoNoEnviado")
    void erroresHttp() {
        assertThatThrownBy(() -> enviar(responde(HttpStatus.UNAUTHORIZED, "{}"))).hasMessageContaining("HTTP 401");
        assertThatThrownBy(() -> enviar(responde(HttpStatus.BAD_GATEWAY, "{}"))).hasMessageContaining("HTTP 502");
    }

    private static ClientHttpRequestFactory falla(RuntimeException error) {
        return (uri, method) -> {
            throw error;
        };
    }

    @Test
    @DisplayName("Fallas inesperadas distinguen timeouts por tipo o mensaje en la cadena de causas")
    void fallasInesperadas() {
        assertThatThrownBy(() -> enviar(falla(new IllegalStateException("Read timed out"))))
                .hasMessageContaining("Tiempo de espera");
        assertThatThrownBy(() -> enviar(falla(new IllegalStateException("Connect TIMEOUT"))))
                .hasMessageContaining("Tiempo de espera");
        assertThatThrownBy(() -> enviar(falla(new IllegalStateException(null, new SocketTimeoutException()))))
                .hasMessageContaining("Tiempo de espera");
        assertThatThrownBy(() -> enviar(falla(new IllegalStateException("conexión rechazada"))))
                .hasMessageContaining("Error al enviar el correo");
    }

    @Test
    @DisplayName("Enmascarar correo nunca expone el usuario completo")
    void enmascararCorreo() {
        assertThat(BrevoEmailClient.enmascararCorreo(null)).isEqualTo("***");
        assertThat(BrevoEmailClient.enmascararCorreo(" ")).isEqualTo("***");
        assertThat(BrevoEmailClient.enmascararCorreo("@dominio.com")).isEqualTo("***");
        assertThat(BrevoEmailClient.enmascararCorreo("sinarroba")).isEqualTo("***");
        assertThat(BrevoEmailClient.enmascararCorreo(" juan@dominio.com ")).isEqualTo("j***@dominio.com");
    }
}
