package com.kapm.onboarding_clientes.client.puntored;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kapm.onboarding_clientes.client.puntored.model.ConfirmacionTx;
import com.kapm.onboarding_clientes.client.puntored.model.PuntoRedMensaje;
import com.kapm.onboarding_clientes.client.puntored.model.PuntoRedProductosResponse;
import com.kapm.onboarding_clientes.client.puntored.model.PuntoRedTxResponse;
import com.kapm.onboarding_clientes.client.puntored.model.SendTxPayload;
import com.kapm.onboarding_clientes.config.PuntoRedProperties;
import com.kapm.onboarding_clientes.exception.PuntoRedAutenticacionException;
import com.kapm.onboarding_clientes.exception.PuntoRedIntegracionException;
import com.kapm.onboarding_clientes.exception.PuntoRedRespuestaInvalidaException;
import com.kapm.onboarding_clientes.exception.PuntoRedTimeoutException;
import com.kapm.onboarding_clientes.service.AesEncryptionService;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.apache.hc.core5.http.ConnectionClosedException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("PuntoRedClient: autenticación, errores de red y respuestas atípicas")
class PuntoRedClientCasosTest {

    private static final String KEY = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";
    private static final String IV = "MDEyMzQ1Njc4OWFiY2RlZg==";

    private MockWebServer server;
    private PuntoRedProperties properties;
    private AesEncryptionService aes;
    /** Falla que se lanza al abrir cualquier conexión que no sea de autenticación (null = conexión real). */
    private final AtomicReference<Exception> fallaServicio = new AtomicReference<>();

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        properties = new PuntoRedProperties();
        properties.setBaseUrl(server.url("/").toString());
        properties.setIdDistribuidor("12345");
        properties.setAesKey(KEY);
        properties.setAesIv(IV);
        aes = new AesEncryptionService(properties);
        aes.init();
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
    }

    private PuntoRedClient cliente(PuntoRedXmlParser parser, AesEncryptionService cifrado) {
        SimpleClientHttpRequestFactory real = new SimpleClientHttpRequestFactory();
        real.setReadTimeout(5000);
        ClientHttpRequestFactory factory = (uri, method) -> {
            Exception falla = fallaServicio.get();
            if (falla != null && !uri.getPath().contains("authenticate")) {
                if (falla instanceof IOException io) {
                    throw io;
                }
                throw (RuntimeException) falla;
            }
            return real.createRequest(uri, method);
        };
        RestClient restClient = RestClient.builder().baseUrl(properties.getBaseUrl()).requestFactory(factory).build();
        return new PuntoRedClient(restClient, properties, cifrado, parser, new ObjectMapper());
    }

    private PuntoRedClient cliente() {
        return cliente(new PuntoRedXmlParser(), aes);
    }

    private static String b64(String json) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }

    private static String jwt(long exp) {
        return b64("{\"alg\":\"HS256\"}") + "." + b64("{\"exp\":" + exp + "}") + ".firma";
    }

    private static MockResponse json(String cuerpo) {
        return new MockResponse().setResponseCode(200).setHeader("Content-Type", "application/json").setBody(cuerpo);
    }

    private static MockResponse xml(String cuerpo) {
        return new MockResponse().setResponseCode(200).setHeader("Content-Type", "application/xml").setBody(cuerpo);
    }

    private void encolarToken() {
        server.enqueue(json("{\"success\":true,\"token\":\"" + jwt(Instant.now().plusSeconds(3600).getEpochSecond()) + "\"}"));
    }

    private static SendTxPayload payload() {
        return SendTxPayload.builder().idProducto(1).idServicio(2).telefono("5512345678")
                .horaLocal("20261023190000").upc("UPC123").unidad("SUCURSAL").build();
    }

    // ------------------------------------------------------------------ autenticación

    @Test
    @DisplayName("Respuestas de autenticación vacías, sin success, sin token o con token vacío se rechazan")
    void autenticacionRechazada() {
        PuntoRedClient client = cliente();
        server.enqueue(json(""));
        assertThatThrownBy(client::obtenerToken).isInstanceOf(PuntoRedAutenticacionException.class).hasMessageContaining("vacía");
        server.enqueue(json("   "));
        assertThatThrownBy(client::obtenerToken).hasMessageContaining("vacía");
        server.enqueue(json("{\"token\":\"abc\"}"));
        assertThatThrownBy(client::obtenerToken).hasMessageContaining("Credenciales no autorizadas");
        server.enqueue(json("{\"success\":true,\"message\":\"sin token\"}"));
        assertThatThrownBy(client::obtenerToken).hasMessageContaining("sin token");
        server.enqueue(json("{\"success\":true,\"token\":\" \"}"));
        assertThatThrownBy(client::obtenerToken).isInstanceOf(PuntoRedAutenticacionException.class);
    }

    @Test
    @DisplayName("JSON ilegible o HTTP 500 en autenticación se reportan como error de integración")
    void autenticacionErrorGenerico() {
        PuntoRedClient client = cliente();
        server.enqueue(json("{no-json"));
        assertThatThrownBy(client::obtenerToken).isExactlyInstanceOf(PuntoRedIntegracionException.class);
        server.enqueue(new MockResponse().setResponseCode(500));
        assertThatThrownBy(client::obtenerToken).isExactlyInstanceOf(PuntoRedIntegracionException.class);
    }

    @Test
    @DisplayName("Sin authPath configurado usa la ruta oficial y respeta API key en blanco")
    void rutaPorDefecto() throws InterruptedException {
        properties.setAuthPath(null);
        properties.setApiKey(" ");
        server.enqueue(json("{\"success\":true,\"token\":\"sin-puntos\"}"));
        assertThat(cliente().obtenerToken()).isEqualTo("sin-puntos");
        RecordedRequest req = server.takeRequest();
        assertThat(req.getPath()).startsWith("/sistema/app/jwt-gp/authenticate/");
        assertThat(req.getHeader("X-API-Key")).isNull();
    }

    @Test
    @DisplayName("Tokens sin exp (o con payload ilegible) no caducan; los que vencen en < 60 s se renuevan")
    void caducidadToken() {
        PuntoRedClient client = cliente();
        String sinExp = b64("{}") + "." + b64("{\"sub\":\"1\"}") + ".f";
        server.enqueue(json("{\"success\":true,\"token\":\"" + sinExp + "\"}"));
        assertThat(client.obtenerToken()).isEqualTo(sinExp);
        assertThat(client.obtenerToken()).isEqualTo(sinExp);

        client.invalidarToken();
        server.enqueue(json("{\"success\":true,\"token\":\"a.!!no-base64!!.c\"}"));
        assertThat(client.obtenerToken()).isEqualTo("a.!!no-base64!!.c");
        assertThat(client.obtenerToken()).isEqualTo("a.!!no-base64!!.c");

        client.invalidarToken();
        String porVencer = jwt(Instant.now().plusSeconds(30).getEpochSecond());
        String nuevo = jwt(Instant.now().plusSeconds(3600).getEpochSecond());
        server.enqueue(json("{\"success\":true,\"token\":\"" + porVencer + "\"}"));
        server.enqueue(json("{\"success\":true,\"token\":\"" + nuevo + "\"}"));
        assertThat(client.obtenerToken()).isEqualTo(porVencer);
        assertThat(client.obtenerToken()).isEqualTo(nuevo);
        assertThat(server.getRequestCount()).isEqualTo(4);
    }

    @Test
    @DisplayName("Dos hilos sin token: solo uno autentica y el otro reutiliza el token recién obtenido")
    void autenticacionConcurrente() throws Exception {
        CountDownLatch llegoPeticion = new CountDownLatch(1);
        CountDownLatch liberar = new CountDownLatch(1);
        String token = jwt(Instant.now().plusSeconds(3600).getEpochSecond());
        server.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) throws InterruptedException {
                llegoPeticion.countDown();
                liberar.await();
                return json("{\"success\":true,\"token\":\"" + token + "\"}");
            }
        });
        PuntoRedClient client = cliente();
        AtomicReference<String> primero = new AtomicReference<>();
        AtomicReference<String> segundo = new AtomicReference<>();

        Thread hilo1 = new Thread(() -> primero.set(client.obtenerToken()));
        hilo1.start();
        llegoPeticion.await();
        Thread hilo2 = new Thread(() -> segundo.set(client.obtenerToken()));
        hilo2.start();
        while (hilo2.getState() != Thread.State.BLOCKED) {
            Thread.onSpinWait();
        }
        liberar.countDown();
        hilo1.join();
        hilo2.join();

        assertThat(primero.get()).isEqualTo(token);
        assertThat(segundo.get()).isEqualTo(token);
        assertThat(server.getRequestCount()).isEqualTo(1);
    }

    // ------------------------------------------------------------------ getProductList

    @Test
    @DisplayName("Catálogo: red caída es timeout y cualquier otra falla es error de integración")
    void catalogoFallas() {
        PuntoRedClient client = cliente();
        encolarToken();
        fallaServicio.set(new IOException("connection refused"));
        assertThatThrownBy(client::getProductList).isInstanceOf(PuntoRedTimeoutException.class);
        fallaServicio.set(new IllegalStateException("boom"));
        assertThatThrownBy(client::getProductList).isExactlyInstanceOf(PuntoRedIntegracionException.class);
        fallaServicio.set(null);
        server.enqueue(xml("<<no-xml"));
        assertThatThrownBy(client::getProductList).isInstanceOf(PuntoRedRespuestaInvalidaException.class);
    }

    @Test
    @DisplayName("Catálogo: tolera respuesta sin MENSAJE ni lista de productos")
    void catalogoSinMensaje() {
        PuntoRedXmlParser parser = mock(PuntoRedXmlParser.class);
        PuntoRedProductosResponse vacia = new PuntoRedProductosResponse();
        vacia.setProductos(null);
        when(parser.parseProductos(any())).thenReturn(vacia);
        encolarToken();
        server.enqueue(xml("<PRODUCTOS/>"));
        assertThat(cliente(parser, aes).getProductList()).isSameAs(vacia);
    }

    // ------------------------------------------------------------------ sendTx

    @Test
    @DisplayName("sendTx: payload nulo y falla de cifrado")
    void sendTxPreparacion() {
        assertThatThrownBy(() -> cliente().sendTx(null)).isInstanceOf(PuntoRedIntegracionException.class);
        AesEncryptionService sinLlaves = new AesEncryptionService(new PuntoRedProperties());
        assertThatThrownBy(() -> cliente(new PuntoRedXmlParser(), sinLlaves).sendTx(payload()))
                .hasMessageContaining("cifrar");
        assertThat(server.getRequestCount()).isZero();
    }

    static Stream<Exception> fallasTimeout() {
        return Stream.of(
                new IOException("refused"),
                new IllegalStateException("wrapper", new SocketTimeoutException()),
                new IllegalStateException("espera", new TimeoutException()),
                new IllegalStateException("cerrada", new ConnectionClosedException()),
                new IllegalStateException("Read timed out"),
                new IllegalStateException("socket TIME OUT"),
                new IllegalStateException("gateway timeout"));
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("fallasTimeout")
    @DisplayName("sendTx: fallas de red o tiempo en cualquier nivel de la causa son resultado DESCONOCIDO")
    void sendTxTimeouts(Exception falla) {
        PuntoRedClient client = cliente();
        encolarToken();
        fallaServicio.set(falla);
        assertThatThrownBy(() -> client.sendTx(payload())).isInstanceOf(PuntoRedTimeoutException.class);
    }

    @Test
    @DisplayName("sendTx: falla sin indicios de timeout es error de integración")
    void sendTxErrorGenerico() {
        PuntoRedClient client = cliente();
        encolarToken();
        fallaServicio.set(new IllegalStateException((String) null));
        assertThatThrownBy(() -> client.sendTx(payload())).isExactlyInstanceOf(PuntoRedIntegracionException.class);
    }

    @Test
    @DisplayName("sendTx: CODIGO 82 toma el ID_TX de la raíz; sin CODIGO la respuesta es inválida")
    void sendTxCodigosAtipicos() {
        PuntoRedClient client = cliente();
        encolarToken();
        server.enqueue(xml("<RESPUESTA><ID_TX>900</ID_TX><MENSAJE><CODIGO>82</CODIGO></MENSAJE></RESPUESTA>"));
        assertThatThrownBy(() -> client.sendTx(payload()))
                .isInstanceOfSatisfying(PuntoRedTimeoutException.class, e -> assertThat(e.getIdTx()).isEqualTo("900"));
        server.enqueue(xml("<MENSAJE><CODIGO>82</CODIGO><ID_TX>902</ID_TX></MENSAJE>"));
        assertThatThrownBy(() -> client.sendTx(payload()))
                .isInstanceOfSatisfying(PuntoRedTimeoutException.class, e -> assertThat(e.getIdTx()).isEqualTo("902"));
        server.enqueue(xml("<RESPUESTA><ID_TX>901</ID_TX></RESPUESTA>"));
        assertThatThrownBy(() -> client.sendTx(payload())).isInstanceOf(PuntoRedRespuestaInvalidaException.class);
    }

    @Test
    @DisplayName("sendTx: CODIGO en blanco se considera respuesta inválida")
    void sendTxCodigoEnBlanco() {
        PuntoRedXmlParser parser = mock(PuntoRedXmlParser.class);
        when(parser.parseSendTx(any())).thenReturn(PuntoRedTxResponse.builder().mensaje(new PuntoRedMensaje(" ", "t")).build());
        encolarToken();
        server.enqueue(xml("<RESPUESTA/>"));
        assertThatThrownBy(() -> cliente(parser, aes).sendTx(payload())).isInstanceOf(PuntoRedRespuestaInvalidaException.class);
    }

    // ------------------------------------------------------------------ confirmTx

    @Test
    @DisplayName("confirmTx: requiere ID_TX")
    void confirmTxSinId() {
        PuntoRedClient client = cliente();
        assertThatThrownBy(() -> client.confirmTx(null)).isInstanceOf(PuntoRedIntegracionException.class);
        assertThatThrownBy(() -> client.confirmTx(" ")).isInstanceOf(PuntoRedIntegracionException.class);
    }

    @Test
    @DisplayName("confirmTx: renueva el token ante 401 y completa el ID_TX desde el MENSAJE o la petición")
    void confirmTxRespuestas() {
        PuntoRedClient client = cliente();
        encolarToken();
        server.enqueue(new MockResponse().setResponseCode(401));
        encolarToken();
        server.enqueue(xml("<MENSAJE><CODIGO>03</CODIGO><ID_TX>77</ID_TX></MENSAJE>"));
        ConfirmacionTx noEncontrada = client.confirmTx(" 77 ");
        assertThat(noEncontrada.estado()).isEqualTo(ConfirmacionTx.Estado.NO_ENCONTRADA);
        assertThat(noEncontrada.respuesta().getIdTx()).isEqualTo("77");

        server.enqueue(xml("<MENSAJE><CODIGO>04</CODIGO></MENSAJE>"));
        ConfirmacionTx pendiente = client.confirmTx("55");
        assertThat(pendiente.estado()).isEqualTo(ConfirmacionTx.Estado.PENDIENTE);
        assertThat(pendiente.respuesta().getIdTx()).isEqualTo("55");

        server.enqueue(xml("<RESPUESTA><ID_TX>56</ID_TX></RESPUESTA>"));
        ConfirmacionTx sinMensaje = client.confirmTx("56");
        assertThat(sinMensaje.estado()).isEqualTo(ConfirmacionTx.Estado.PENDIENTE);
        assertThat(sinMensaje.respuesta().getIdTx()).isEqualTo("56");

        server.enqueue(xml("<RESPUESTA><NUM_AUTORIZACION>A9</NUM_AUTORIZACION></RESPUESTA>"));
        ConfirmacionTx soloAutorizacion = client.confirmTx("57");
        assertThat(soloAutorizacion.estado()).isEqualTo(ConfirmacionTx.Estado.PENDIENTE);
        assertThat(soloAutorizacion.respuesta().getIdTx()).isNull();
    }

    @Test
    @DisplayName("confirmTx: XML ilegible, timeout y error genérico")
    void confirmTxFallas() {
        PuntoRedClient client = cliente();
        encolarToken();
        server.enqueue(xml("<<no-xml"));
        assertThatThrownBy(() -> client.confirmTx("1")).isInstanceOf(PuntoRedRespuestaInvalidaException.class);
        fallaServicio.set(new IOException("refused"));
        assertThatThrownBy(() -> client.confirmTx("1")).isInstanceOf(PuntoRedTimeoutException.class);
        fallaServicio.set(new IllegalStateException("boom"));
        assertThatThrownBy(() -> client.confirmTx("1")).isExactlyInstanceOf(PuntoRedIntegracionException.class);
    }
}
