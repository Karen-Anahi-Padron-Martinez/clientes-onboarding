package com.kapm.onboarding_clientes.client.brevo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kapm.onboarding_clientes.config.BrevoProperties;
import com.kapm.onboarding_clientes.exception.CorreoNoEnviadoException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import okhttp3.mockwebserver.SocketPolicy;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.util.Timeout;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Pruebas de Integración y MockWebServer - BrevoEmailClient")
class BrevoEmailClientTest {

    private MockWebServer mockWebServer;
    private BrevoProperties properties;
    private BrevoEmailClient brevoEmailClient;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        properties = new BrevoProperties();
        properties.setBaseUrl(mockWebServer.url("/").toString());
        properties.setApiKey("xkeysib-test-api-key-12345");
        properties.setSenderEmail("notificaciones@bancodigital.com");
        properties.setSenderName("Banco Digital");
        properties.setConnectTimeout(2000);
        properties.setReadTimeout(3000);

        objectMapper = new ObjectMapper();

        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(Timeout.ofMilliseconds(properties.getConnectTimeout()))
                .setResponseTimeout(Timeout.ofMilliseconds(properties.getReadTimeout()))
                .build();

        CloseableHttpClient httpClient = HttpClients.custom()
                .setDefaultRequestConfig(requestConfig)
                .build();

        RestClient restClient = RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .requestFactory(new HttpComponentsClientHttpRequestFactory(httpClient))
                .build();

        brevoEmailClient = new BrevoEmailClient(restClient, properties, objectMapper);
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    @DisplayName("Envío exitoso de correo transaccional (HTTP 201 Created) con headers y body correctos")
    void testEnvioCorreoExitoso() throws Exception {
        String expectedMessageId = "<202610231900.123456@smtp-relay.brevo.com>";
        String jsonResponse = "{\"messageId\":\"" + expectedMessageId + "\"}";

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(201)
                .addHeader("Content-Type", "application/json")
                .setBody(jsonResponse));

        String destinatario = "juan.perez@dominio.com";
        String asunto = "Tu código de seguridad 2FA";
        String html = "<p>Tu código de verificación es: <strong>839201</strong></p>";

        String messageId = brevoEmailClient.enviarCorreo(destinatario, asunto, html);

        assertThat(messageId).isEqualTo(expectedMessageId);

        // Validar la solicitud enviada por RestClient a MockWebServer
        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertThat(recordedRequest.getMethod()).isEqualTo("POST");
        assertThat(recordedRequest.getPath()).isEqualTo("/v3/smtp/email");
        assertThat(recordedRequest.getHeader("api-key")).isEqualTo("xkeysib-test-api-key-12345");
        assertThat(recordedRequest.getHeader("Content-Type")).contains("application/json");

        // Validar el payload JSON
        JsonNode bodyJson = objectMapper.readTree(recordedRequest.getBody().readUtf8());
        assertThat(bodyJson.has("sender")).isTrue();
        assertThat(bodyJson.get("sender").get("email").asText()).isEqualTo("notificaciones@bancodigital.com");
        assertThat(bodyJson.get("sender").get("name").asText()).isEqualTo("Banco Digital");

        assertThat(bodyJson.has("to")).isTrue();
        assertThat(bodyJson.get("to").get(0).get("email").asText()).isEqualTo("juan.perez@dominio.com");

        assertThat(bodyJson.get("subject").asText()).isEqualTo(asunto);
        assertThat(bodyJson.get("htmlContent").asText()).isEqualTo(html);
    }

    @Test
    @DisplayName("Error 401 Unauthorized lanza CorreoNoEnviadoException con código en ErrorMessage")
    void testError401Unauthorized() {
        String jsonError = "{\"code\":\"unauthorized\",\"message\":\"Key not found\"}";

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(401)
                .addHeader("Content-Type", "application/json")
                .setBody(jsonError));

        assertThatThrownBy(() -> brevoEmailClient.enviarCorreo("cliente@banco.com", "Asunto", "<p>Texto</p>"))
                .isInstanceOf(CorreoNoEnviadoException.class)
                .hasMessageContaining("401")
                .satisfies(ex -> {
                    CorreoNoEnviadoException cne = (CorreoNoEnviadoException) ex;
                    assertThat(cne.getCodigo()).isEqualTo(99);
                });
    }

    @Test
    @DisplayName("Timeout de red al conectar con Brevo lanza CorreoNoEnviadoException")
    void testTimeoutConexionLanzaCorreoNoEnviadoException() {
        mockWebServer.enqueue(new MockResponse()
                .setSocketPolicy(SocketPolicy.NO_RESPONSE));

        assertThatThrownBy(() -> brevoEmailClient.enviarCorreo("cliente@banco.com", "Asunto", "<p>Texto</p>"))
                .isInstanceOf(CorreoNoEnviadoException.class)
                .hasMessageContaining("Tiempo de espera")
                .satisfies(ex -> {
                    CorreoNoEnviadoException cne = (CorreoNoEnviadoException) ex;
                    assertThat(cne.getCodigo()).isEqualTo(99);
                });
    }

    @Test
    @DisplayName("Error 400 Bad Request lanza CorreoNoEnviadoException")
    void testError400BadRequest() {
        String jsonError = "{\"code\":\"invalid_parameter\",\"message\":\"Invalid email address\"}";

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(400)
                .addHeader("Content-Type", "application/json")
                .setBody(jsonError));

        assertThatThrownBy(() -> brevoEmailClient.enviarCorreo("correo_invalido@dominio.com", "Asunto", "<p>Texto</p>"))
                .isInstanceOf(CorreoNoEnviadoException.class)
                .hasMessageContaining("400");
    }

    @Test
    @DisplayName("Error 500 Internal Server Error lanza CorreoNoEnviadoException")
    void testError500InternalServerError() {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(500)
                .setBody("Error interno del servidor Brevo"));

        assertThatThrownBy(() -> brevoEmailClient.enviarCorreo("cliente@banco.com", "Asunto", "<p>Texto</p>"))
                .isInstanceOf(CorreoNoEnviadoException.class)
                .hasMessageContaining("500");
    }

    @ParameterizedTest
    @CsvSource({
            "juan.perez@dominio.com, j***@dominio.com",
            "karen@gmail.com, k***@gmail.com",
            "notificaciones@bancodigital.com, n***@bancodigital.com",
            "a@test.org, a***@test.org"
    })
    @DisplayName("Enmascaramiento de correo electrónico genera el formato j***@dominio.com")
    void testEnmascararCorreo(String emailOriginal, String emailEsperado) {
        assertThat(BrevoEmailClient.enmascararCorreo(emailOriginal)).isEqualTo(emailEsperado);
    }

    @Test
    @DisplayName("Enmascaramiento de correos nulos o vacíos retorna asteriscos seguros")
    void testEnmascararCorreoCasosBorde() {
        assertThat(BrevoEmailClient.enmascararCorreo(null)).isEqualTo("***");
        assertThat(BrevoEmailClient.enmascararCorreo("")).isEqualTo("***");
        assertThat(BrevoEmailClient.enmascararCorreo("invalido")).isEqualTo("***");
    }

    @Test
    @DisplayName("Parámetros obligatorios vacíos o nulos lanzan CorreoNoEnviadoException")
    void testValidacionParametrosObligatorios() {
        assertThatThrownBy(() -> brevoEmailClient.enviarCorreo(null, "Asunto", "<p>Html</p>"))
                .isInstanceOf(CorreoNoEnviadoException.class)
                .hasMessageContaining("destinatario");

        assertThatThrownBy(() -> brevoEmailClient.enviarCorreo("cliente@banco.com", null, "<p>Html</p>"))
                .isInstanceOf(CorreoNoEnviadoException.class)
                .hasMessageContaining("asunto");

        assertThatThrownBy(() -> brevoEmailClient.enviarCorreo("cliente@banco.com", "Asunto", null))
                .isInstanceOf(CorreoNoEnviadoException.class)
                .hasMessageContaining("HTML");
    }

    @Test
    @DisplayName("Falta de API key en BrevoProperties lanza CorreoNoEnviadoException")
    void testFaltaApiKeyLanzaExcepcion() {
        properties.setApiKey(null);

        assertThatThrownBy(() -> brevoEmailClient.enviarCorreo("cliente@banco.com", "Asunto", "<p>Html</p>"))
                .isInstanceOf(CorreoNoEnviadoException.class)
                .hasMessageContaining("API key");
    }
}
