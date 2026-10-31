package com.kapm.onboarding_clientes.client.puntored;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kapm.onboarding_clientes.client.puntored.model.*;
import com.kapm.onboarding_clientes.config.PuntoRedProperties;
import com.kapm.onboarding_clientes.exception.PuntoRedAutenticacionException;
import com.kapm.onboarding_clientes.exception.PuntoRedRechazoException;
import com.kapm.onboarding_clientes.exception.PuntoRedTransaccionDuplicadaException;
import com.kapm.onboarding_clientes.exception.PuntoRedRespuestaInvalidaException;
import com.kapm.onboarding_clientes.exception.PuntoRedTimeoutException;
import com.kapm.onboarding_clientes.service.AesEncryptionService;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import okhttp3.mockwebserver.SocketPolicy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Pruebas de Integración y MockWebServer - PuntoRedClient")
class PuntoRedClientTest {

    private static final String SECRET_PASSWORD = "PasswordUltraSecreto123!";
    private static final String VALID_KEY_BASE64 = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";
    private static final String VALID_IV_BASE64 = "MDEyMzQ1Njc4OWFiY2RlZg==";

    private MockWebServer server;
    private PuntoRedProperties properties;
    private PuntoRedClient client;
    private ListAppender<ILoggingEvent> logAppender;
    private Logger clientLogger;
    private AesEncryptionService aesService;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();

        String baseUrl = server.url("/").toString();

        properties = new PuntoRedProperties();
        properties.setBaseUrl(baseUrl);
        properties.setAuthPath("/sistema/app/jwt-gp/authenticate/");
        properties.setIdDistribuidor("12345");
        properties.setCodigoDispositivo("POS-001");
        properties.setPassword(SECRET_PASSWORD);
        properties.setUnidad("SUCURSAL_CENTRO");
        properties.setAesKey(VALID_KEY_BASE64);
        properties.setAesIv(VALID_IV_BASE64);
        properties.setConnectTimeout(1000);
        properties.setReadTimeout(1000);

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(1000);
        requestFactory.setReadTimeout(1000);

        RestClient restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();

        aesService = new AesEncryptionService(properties);
        aesService.init();

        PuntoRedXmlParser xmlParser = new PuntoRedXmlParser();
        ObjectMapper objectMapper = new ObjectMapper();

        client = new PuntoRedClient(restClient, properties, aesService, xmlParser, objectMapper);

        // Configurar captura de logs para verificar seguridad
        clientLogger = (Logger) LoggerFactory.getLogger(PuntoRedClient.class);
        logAppender = new ListAppender<>();
        logAppender.start();
        clientLogger.addAppender(logAppender);
        clientLogger.setLevel(Level.DEBUG);
    }

    @AfterEach
    void tearDown() throws IOException {
        if (server != null) {
            server.shutdown();
        }
        if (clientLogger != null && logAppender != null) {
            clientLogger.detachAppender(logAppender);
        }
    }

    private String crearJwtMock(long expEpochSeconds) {
        String header = Base64.getUrlEncoder().withoutPadding().encodeToString("{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));
        String payload = Base64.getUrlEncoder().withoutPadding().encodeToString(("{\"sub\":\"12345\",\"exp\":" + expEpochSeconds + "}").getBytes(StandardCharsets.UTF_8));
        String signature = "mockSignature123";
        return header + "." + payload + "." + signature;
    }

    @Test
    @DisplayName("1. Authenticate OK: Obtiene y cachea el token en memoria")
    void testAuthenticateOk() throws InterruptedException {
        String mockJwt = crearJwtMock(Instant.now().plusSeconds(3600).getEpochSecond());
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"success\":true,\"token\":\"" + mockJwt + "\"}"));

        String token1 = client.obtenerToken();
        assertThat(token1).isEqualTo(mockJwt);

        // Segunda llamada debe consumir la caché en memoria sin invocar nuevamente el servidor
        String token2 = client.obtenerToken();
        assertThat(token2).isEqualTo(mockJwt);
        assertThat(server.getRequestCount()).isEqualTo(1);

        RecordedRequest req = server.takeRequest(1, TimeUnit.SECONDS);
        assertThat(req).isNotNull();
        assertThat(req.getMethod()).isEqualTo("POST");
        assertThat(req.getPath()).contains("idDistribuidor=12345");
        assertThat(req.getPath()).contains("codigoDispositivo=POS-001");
        assertThat(req.getPath()).contains("password=" + SECRET_PASSWORD);
    }

    @Test
    @DisplayName("2. Authenticate con success=false: Lanza PuntoRedRechazoException")
    void testAuthenticateSuccessFalse() {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"success\":false,\"message\":\"Contraseña incorrecta o distribuidor inactivo\"}"));

        assertThatThrownBy(() -> client.obtenerToken())
                .isInstanceOf(PuntoRedRechazoException.class)
                .hasMessageContaining("Autenticación rechazada por PuntoRed");
    }

    @Test
    @DisplayName("3. HTTP 401 con renovación automática de token y reintento único exitoso")
    void testHttp401RenovacionYReintentoExitoso() throws InterruptedException {
        String tokenInicial = crearJwtMock(Instant.now().plusSeconds(3600).getEpochSecond());
        String tokenNuevo = crearJwtMock(Instant.now().plusSeconds(7200).getEpochSecond());

        // 1. Authenticate inicial
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"success\":true,\"token\":\"" + tokenInicial + "\"}"));

        // 2. getProductList con 401 Unauthorized
        server.enqueue(new MockResponse()
                .setResponseCode(401)
                .setHeader("Content-Type", "text/plain")
                .setBody("Unauthorized"));

        // 3. Re-authenticate
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"success\":true,\"token\":\"" + tokenNuevo + "\"}"));

        // 4. getProductList reintento exitoso con nuevo token
        String xmlExitoso = """
                <PRODUCTOS>
                    <MENSAJE><CODIGO>01</CODIGO><TEXTO>OK</TEXTO></MENSAJE>
                    <PRODUCTO>
                        <producto>CFE</producto>
                        <servicio>CFE SUMINISTRADOR</servicio>
                        <idServicio>1001</idServicio>
                        <idProducto>5001</idProducto>
                        <idCatTipoServicio>2</idCatTipoServicio>
                        <tipoFront>1</tipoFront>
                    </PRODUCTO>
                </PRODUCTOS>
                """;
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/xml")
                .setBody(xmlExitoso));

        PuntoRedProductosResponse response = client.getProductList();

        assertThat(response).isNotNull();
        assertThat(response.getProductos()).hasSize(1);
        assertThat(response.getProductos().get(0).getProducto()).isEqualTo("CFE");
        assertThat(server.getRequestCount()).isEqualTo(4);

        // Verificar que el reintento utilizó el Bearer del nuevo token
        server.takeRequest(); // Auth 1
        server.takeRequest(); // Req 1 (401)
        server.takeRequest(); // Auth 2
        RecordedRequest reqReintento = server.takeRequest(); // Req 2
        assertThat(reqReintento.getHeader("Authorization")).isEqualTo("Bearer " + tokenNuevo);
    }

    @Test
    @DisplayName("4. getProductList OK: Parsea correctamente el XML con múltiples productos")
    void testGetProductListOk() {
        String token = crearJwtMock(Instant.now().plusSeconds(3600).getEpochSecond());
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"success\":true,\"token\":\"" + token + "\"}"));

        String xml = """
                <PRODUCTOS>
                    <MENSAJE>
                        <CODIGO>01</CODIGO>
                        <TEXTO>CONSULTA EXITOSA</TEXTO>
                    </MENSAJE>
                    <PRODUCTO>
                        <producto>TELMEX</producto>
                        <servicio>TELEFONIA E INTERNET</servicio>
                        <idServicio>2001</idServicio>
                        <idProducto>6001</idProducto>
                        <idCatTipoServicio>1</idCatTipoServicio>
                        <tipoFront>1</tipoFront>
                    </PRODUCTO>
                    <PRODUCTO>
                        <producto>TELCEL</producto>
                        <servicio>RECARGA TIEMPO AIRE</servicio>
                        <idServicio>3001</idServicio>
                        <idProducto>7001</idProducto>
                        <idCatTipoServicio>3</idCatTipoServicio>
                        <tipoFront>0</tipoFront>
                    </PRODUCTO>
                </PRODUCTOS>
                """;
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/xml")
                .setBody(xml));

        PuntoRedProductosResponse response = client.getProductList();

        assertThat(response).isNotNull();
        assertThat(response.getMensaje().getCodigo()).isEqualTo("01");
        assertThat(response.getProductos()).hasSize(2);
        assertThat(response.getProductos().get(0).getProducto()).isEqualTo("TELMEX");
        assertThat(response.getProductos().get(1).getProducto()).isEqualTo("TELCEL");
    }

    @Test
    @DisplayName("5. sendTx OK: Cifra payload y procesa respuesta XML con FECHA parseada con Locale.ENGLISH")
    void testSendTxOk() {
        String token = crearJwtMock(Instant.now().plusSeconds(3600).getEpochSecond());
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"success\":true,\"token\":\"" + token + "\"}"));

        String xmlSendTx = """
                <RESPUESTA>
                    <MENSAJE>
                        <CODIGO>01</CODIGO>
                        <TEXTO>TRANSACCION EXITOSA</TEXTO>
                    </MENSAJE>
                    <ID_TX>TX-987654321</ID_TX>
                    <NUM_AUTORIZACION>AUTH-009988</NUM_AUTORIZACION>
                    <SALDO>15420.50</SALDO>
                    <COMISION>12.00</COMISION>
                    <SALDO_F>15,420.50</SALDO_F>
                    <COMISION_F>12.00</COMISION_F>
                    <FECHA>25/Feb/2022 18:15:25</FECHA>
                    <MONTO>350.00</MONTO>
                </RESPUESTA>
                """;
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/xml")
                .setBody(xmlSendTx));

        SendTxPayload payload = SendTxPayload.builder()
                .idProducto(5001)
                .idServicio(1001)
                .telefono("5512345678")
                .horaLocal("20261023180000")
                .referencia("REF-CFE-009876")
                .montoPago(new BigDecimal("350.00"))
                .upc("UPC-9901")
                .unidad("SUCURSAL_CENTRO")
                .tipoFront(1)
                .build();

        PuntoRedTxResponse txResponse = client.sendTx(payload);

        assertThat(txResponse).isNotNull();
        assertThat(txResponse.getIdTx()).isEqualTo("TX-987654321");
        assertThat(txResponse.getNumAutorizacion()).isEqualTo("AUTH-009988");
        assertThat(txResponse.getSaldo()).isEqualByComparingTo("15420.50");
        assertThat(txResponse.getMonto()).isEqualByComparingTo("350.00");
        assertThat(txResponse.getFecha()).isEqualTo("25/Feb/2022 18:15:25");

        // Verificación de parseo con Locale.ENGLISH
        assertThat(txResponse.getFechaParsed()).isNotNull();
        assertThat(txResponse.getFechaParsed().getYear()).isEqualTo(2022);
        assertThat(txResponse.getFechaParsed().getMonthValue()).isEqualTo(2);
        assertThat(txResponse.getFechaParsed().getDayOfMonth()).isEqualTo(25);
    }

    @Test
    @DisplayName("6. sendTx con rechazo (CODIGO != 01): Lanza PuntoRedRechazoException preservando código y texto")
    void testSendTxRechazoCodigoDistinto01() {
        String token = crearJwtMock(Instant.now().plusSeconds(3600).getEpochSecond());
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"success\":true,\"token\":\"" + token + "\"}"));

        String xmlRechazo = """
                <RESPUESTA>
                    <MENSAJE>
                        <CODIGO>02</CODIGO>
                        <TEXTO>SALDO INSUFICIENTE EN BOLSA DEL DISTRIBUIDOR</TEXTO>
                    </MENSAJE>
                    <ID_TX>TX-ERROR-001</ID_TX>
                </RESPUESTA>
                """;
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/xml")
                .setBody(xmlRechazo));

        SendTxPayload payload = SendTxPayload.builder()
                .idProducto(5001)
                .idServicio(1001)
                .telefono("5512345678")
                .horaLocal("20261023180000")
                .referencia("REF-CFE-009876")
                .montoPago(new BigDecimal("100.00"))
                .upc("UPC-9902")
                .unidad("SUCURSAL_CENTRO")
                .tipoFront(1)
                .build();

        assertThatThrownBy(() -> client.sendTx(payload))
                .isInstanceOf(PuntoRedRechazoException.class)
                .satisfies(ex -> {
                    PuntoRedRechazoException rechazo = (PuntoRedRechazoException) ex;
                    assertThat(rechazo.getCodigoPuntoRed()).isEqualTo("02");
                    assertThat(rechazo.getTextoPuntoRed()).contains("SALDO INSUFICIENTE");
                });
    }

    @Test
    @DisplayName("7. sendTx con Timeout de red: Lanza PuntoRedTimeoutException (resultado DESCONOCIDO)")
    void testSendTxTimeout() {
        String token = crearJwtMock(Instant.now().plusSeconds(3600).getEpochSecond());
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"success\":true,\"token\":\"" + token + "\"}"));

        // No responder para forzar socket timeout
        server.enqueue(new MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE));

        SendTxPayload payload = SendTxPayload.builder()
                .idProducto(5001)
                .idServicio(1001)
                .telefono("5512345678")
                .horaLocal("20261023180000")
                .referencia("REF-CFE-009876")
                .montoPago(new BigDecimal("100.00"))
                .upc("UPC-TIMEOUT")
                .unidad("SUCURSAL_CENTRO")
                .tipoFront(1)
                .build();

        assertThatThrownBy(() -> client.sendTx(payload))
                .isInstanceOf(PuntoRedTimeoutException.class)
                .hasMessageContaining("Tiempo de espera agotado");
    }

    @Test
    @DisplayName("8. sendTx con XML corrupto o inesperado: Lanza PuntoRedRespuestaInvalidaException")
    void testSendTxXmlCorrupto() {
        String token = crearJwtMock(Instant.now().plusSeconds(3600).getEpochSecond());
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"success\":true,\"token\":\"" + token + "\"}"));

        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/xml")
                .setBody("<HTML><BODY>502 Bad Gateway Nginx</BODY></HTML>"));

        SendTxPayload payload = SendTxPayload.builder()
                .idProducto(5001)
                .idServicio(1001)
                .telefono("5512345678")
                .horaLocal("20261023180000")
                .referencia("REF-CFE-009876")
                .montoPago(new BigDecimal("100.00"))
                .upc("UPC-CORRUPTO")
                .unidad("SUCURSAL_CENTRO")
                .tipoFront(1)
                .build();

        assertThatThrownBy(() -> client.sendTx(payload))
                .isInstanceOf(PuntoRedRespuestaInvalidaException.class)
                .hasMessageContaining("inválida, inesperada");
    }

    @Test
    @DisplayName("9. Seguridad de Logs: Nunca se expone token, contraseña, URL de authenticate, payload en claro ni teléfono completo")
    void testSeguridadLogsNuncaExponeSecretos() {
        String secretToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NSIsImV4cCI6OTk5OTk5OTk5OX0.SUPER_SECRET_SIGNATURE";
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"success\":true,\"token\":\"" + secretToken + "\"}"));

        String xmlOk = """
                <RESPUESTA>
                    <MENSAJE><CODIGO>01</CODIGO><TEXTO>OK</TEXTO></MENSAJE>
                    <ID_TX>TX-LOG-TEST</ID_TX>
                </RESPUESTA>
                """;
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/xml")
                .setBody(xmlOk));

        String telefonoCompleto = "5598765432";
        SendTxPayload payload = SendTxPayload.builder()
                .idProducto(777)
                .idServicio(888)
                .telefono(telefonoCompleto)
                .horaLocal("20261023180000")
                .referencia("REF-CONFIDENCIAL-123")
                .montoPago(new BigDecimal("200.00"))
                .upc("UPC-LOG-01")
                .unidad("SUCURSAL_CENTRO")
                .tipoFront(1)
                .build();

        client.sendTx(payload);

        // Inspeccionar todas las líneas de log generadas
        for (ILoggingEvent logEvent : logAppender.list) {
            String mensaje = logEvent.getFormattedMessage();

            // 1. La contraseña secreta NUNCA debe aparecer
            assertThat(mensaje).doesNotContain(SECRET_PASSWORD);

            // 2. El token JWT NUNCA debe aparecer
            assertThat(mensaje).doesNotContain(secretToken);

            // 3. La URL de authenticate con password en query param NUNCA debe aparecer
            assertThat(mensaje).doesNotContain("password=" + SECRET_PASSWORD);

            // 4. El teléfono completo NUNCA debe aparecer en claro
            assertThat(mensaje).doesNotContain(telefonoCompleto);
        }

        // Verificar que los logs sí contengan idProducto, upc y teléfono enmascarado
        boolean contieneIdProducto = logAppender.list.stream()
                .anyMatch(e -> e.getFormattedMessage().contains("777"));
        boolean contieneUpc = logAppender.list.stream()
                .anyMatch(e -> e.getFormattedMessage().contains("UPC-LOG-01"));
        boolean contieneTelEnmascarado = logAppender.list.stream()
                .anyMatch(e -> e.getFormattedMessage().contains("******5432"));

        assertThat(contieneIdProducto).isTrue();
        assertThat(contieneUpc).isTrue();
        assertThat(contieneTelEnmascarado).isTrue();
    }

    // =========================================================================
    // Formato oficial de sendTx, interpretación de códigos y confirmTx
    // =========================================================================

    private void encolarToken() {
        String token = crearJwtMock(Instant.now().plusSeconds(3600).getEpochSecond());
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"success\":true,\"token\":\"" + token + "\"}"));
    }

    private void encolarXml(String xml) {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/xml")
                .setBody(xml));
    }

    private static String respuestaConCodigo(String codigo, String texto) {
        return "<RESPONSE><ID_TX>291437188</ID_TX><MENSAJE><CODIGO>" + codigo + "</CODIGO><TEXTO>" + texto
                + "</TEXTO></MENSAJE></RESPONSE>";
    }

    private SendTxPayload payloadRecarga(String upc) {
        return SendTxPayload.builder()
                .idProducto(582)
                .idServicio(133)
                .telefono("5555555551")
                .horaLocal("20261023180000")
                .montoPago(new BigDecimal("10.00"))
                .upc(upc)
                .unidad("SUCURSAL_CENTRO")
                .tipoFront(0)
                .build();
    }

    private static String parametro(String cuerpoForm, String nombre) {
        for (String par : cuerpoForm.split("&")) {
            String[] kv = par.split("=", 2);
            if (kv[0].equals(nombre)) {
                return URLDecoder.decode(kv[1], StandardCharsets.UTF_8);
            }
        }
        return null;
    }

    @Test
    @DisplayName("10. sendTx usa x-www-form-urlencoded con 'random' y 'signed' (JSON cifrado AES-256-CBC)")
    void testSendTxFormatoOficial() throws InterruptedException {
        encolarToken();
        encolarXml(respuestaConCodigo("01", "Operacion realizada con exito"));

        client.sendTx(payloadRecarga("UPC-FORM-01"));

        server.takeRequest(); // authenticate
        RecordedRequest req = server.takeRequest(1, TimeUnit.SECONDS);
        assertThat(req.getPath()).isEqualTo("/sistema/service/sendTx.do");
        assertThat(req.getHeader("Content-Type")).startsWith("application/x-www-form-urlencoded");

        String cuerpo = req.getBody().readUtf8();
        assertThat(parametro(cuerpo, "random")).matches("\\d+");

        String json = aesService.decrypt(parametro(cuerpo, "signed"));
        assertThat(json).contains("\"upc\":\"UPC-FORM-01\"").contains("\"idProducto\":582");
        // tipoFront es contexto interno y nunca viaja al proveedor
        assertThat(json).doesNotContain("tipoFront");
        // El payload en claro jamás viaja sin cifrar
        assertThat(cuerpo).doesNotContain("5555555551");
    }

    @Test
    @DisplayName("11. CODIGO 06: lanza PuntoRedTransaccionDuplicadaException (el llamador decide según contexto)")
    void testSendTxCodigo06() {
        encolarToken();
        encolarXml(respuestaConCodigo("06", "Transaccion aplicada anteriormente Autorizacion [100024988]"));

        assertThatThrownBy(() -> client.sendTx(payloadRecarga("UPC-DUP-01")))
                .isInstanceOf(PuntoRedTransaccionDuplicadaException.class)
                .satisfies(ex -> assertThat(((PuntoRedTransaccionDuplicadaException) ex).getCodigoPuntoRed()).isEqualTo("06"));
    }

    @Test
    @DisplayName("12. CODIGO 82: resultado DESCONOCIDO con ID_TX para confirmTx (nunca se trata como rechazo)")
    void testSendTxCodigo82() {
        encolarToken();
        encolarXml(respuestaConCodigo("82", "Tiempo de espera alcanzado, la transaccion no ha sido registrada"));

        assertThatThrownBy(() -> client.sendTx(payloadRecarga("UPC-82-01")))
                .isInstanceOf(PuntoRedTimeoutException.class)
                .isNotInstanceOf(PuntoRedRechazoException.class)
                .satisfies(ex -> assertThat(((PuntoRedTimeoutException) ex).getIdTx()).isEqualTo("291437188"));
    }

    @Test
    @DisplayName("13. Códigos no documentados como éxito ('00') se tratan como rechazo")
    void testSendTxCodigo00NoEsExito() {
        encolarToken();
        encolarXml(respuestaConCodigo("00", "?"));

        assertThatThrownBy(() -> client.sendTx(payloadRecarga("UPC-00-01")))
                .isInstanceOf(PuntoRedRechazoException.class);
    }

    @Test
    @DisplayName("14. HTTP 403 (token vencido según PuntoRed): renueva token y reenvía la MISMA trama cifrada")
    void testSendTx403RenuevaToken() throws InterruptedException {
        encolarToken();
        server.enqueue(new MockResponse().setResponseCode(403).setBody("Forbidden"));
        encolarToken();
        encolarXml(respuestaConCodigo("01", "OK"));

        PuntoRedTxResponse respuesta = client.sendTx(payloadRecarga("UPC-403-01"));

        assertThat(respuesta.getMensaje().getCodigo()).isEqualTo("01");
        server.takeRequest();
        String primerCuerpo = server.takeRequest().getBody().readUtf8();
        server.takeRequest();
        String segundoCuerpo = server.takeRequest().getBody().readUtf8();
        assertThat(segundoCuerpo).isEqualTo(primerCuerpo);
    }

    @Test
    @DisplayName("15. HTTP 403 persistente: PuntoRedAutenticacionException")
    void testSendTx403Persistente() {
        encolarToken();
        server.enqueue(new MockResponse().setResponseCode(403).setBody("Forbidden"));
        encolarToken();
        server.enqueue(new MockResponse().setResponseCode(403).setBody("Forbidden"));

        assertThatThrownBy(() -> client.sendTx(payloadRecarga("UPC-403-02")))
                .isInstanceOf(PuntoRedAutenticacionException.class);
    }

    @Test
    @DisplayName("16. X-API-Key se envía solo cuando está configurada")
    void testApiKeyOpcional() throws InterruptedException {
        properties.setApiKey("API-KEY-PRUEBA");
        encolarToken();
        encolarXml(respuestaConCodigo("01", "OK"));

        client.sendTx(payloadRecarga("UPC-KEY-01"));

        server.takeRequest();
        assertThat(server.takeRequest().getHeader("X-API-Key")).isEqualTo("API-KEY-PRUEBA");

        properties.setApiKey(null);
        encolarXml(respuestaConCodigo("01", "OK"));
        client.sendTx(payloadRecarga("UPC-KEY-02"));
        assertThat(server.takeRequest().getHeader("X-API-Key")).isNull();
    }

    @Test
    @DisplayName("17. confirmTx 01: APLICADA con número de autorización; envía idDistribuidor e idTransaccion")
    void testConfirmTxAplicada() throws InterruptedException {
        encolarToken();
        encolarXml("""
                <RESPONSE>
                    <NUM_AUTORIZACION>100024988</NUM_AUTORIZACION>
                    <MENSAJE><CODIGO>01</CODIGO><TEXTO>Transaccion Aplicada</TEXTO><ID_TX>291437188</ID_TX></MENSAJE>
                </RESPONSE>
                """);

        ConfirmacionTx confirmacion = client.confirmTx("291437188");

        assertThat(confirmacion.estado()).isEqualTo(ConfirmacionTx.Estado.APLICADA);
        assertThat(confirmacion.respuesta().getNumAutorizacion()).isEqualTo("100024988");
        assertThat(confirmacion.respuesta().getIdTx()).isEqualTo("291437188");

        server.takeRequest();
        RecordedRequest req = server.takeRequest();
        assertThat(req.getPath()).isEqualTo("/sistema/service/confirmTx.do");
        String cuerpo = req.getBody().readUtf8();
        assertThat(parametro(cuerpo, "idDistribuidor")).isEqualTo("12345");
        assertThat(parametro(cuerpo, "idTransaccion")).isEqualTo("291437188");
    }

    @Test
    @DisplayName("18. confirmTx 03 -> NO_ENCONTRADA; 04 y códigos desconocidos -> PENDIENTE")
    void testConfirmTxOtrosEstados() {
        encolarToken();
        encolarXml(respuestaConCodigo("03", "Transaccion no encontrada"));
        encolarXml(respuestaConCodigo("04", "Transaccion pendiente"));
        encolarXml(respuestaConCodigo("77", "?"));

        assertThat(client.confirmTx("1").estado()).isEqualTo(ConfirmacionTx.Estado.NO_ENCONTRADA);
        assertThat(client.confirmTx("1").estado()).isEqualTo(ConfirmacionTx.Estado.PENDIENTE);
        assertThat(client.confirmTx("1").estado()).isEqualTo(ConfirmacionTx.Estado.PENDIENTE);
    }

    @Test
    @DisplayName("19. Falla de red al autenticar: ni la excepción ni los logs exponen la contraseña")
    void testAuthFallaNoExponePassword() throws IOException {
        server.shutdown();

        assertThatThrownBy(() -> client.obtenerToken())
                .satisfies(ex -> assertThat(ex.getMessage()).doesNotContain(SECRET_PASSWORD));
        for (ILoggingEvent evento : logAppender.list) {
            assertThat(evento.getFormattedMessage()).doesNotContain(SECRET_PASSWORD);
        }
        server = null;
    }
}
