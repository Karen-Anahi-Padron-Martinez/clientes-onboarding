package com.kapm.onboarding_clientes.client.puntored;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kapm.onboarding_clientes.client.puntored.model.*;
import com.kapm.onboarding_clientes.config.PuntoRedProperties;
import com.kapm.onboarding_clientes.exception.PuntoRedAutenticacionException;
import com.kapm.onboarding_clientes.exception.PuntoRedIntegracionException;
import com.kapm.onboarding_clientes.exception.PuntoRedRechazoException;
import com.kapm.onboarding_clientes.exception.PuntoRedRespuestaInvalidaException;
import com.kapm.onboarding_clientes.exception.PuntoRedTimeoutException;
import com.kapm.onboarding_clientes.exception.PuntoRedTransaccionDuplicadaException;
import com.kapm.onboarding_clientes.service.AesEncryptionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Cliente HTTP transaccional para la comunicación con la plataforma PuntoRed (GestoPago).
 * Administra el ciclo de vida del token JWT, serialización/cifrado AES y consumo de endpoints.
 * <p>
 * Seguridad: ningún mensaje de excepción ni de log incluye la URL de autenticación (lleva la contraseña en el
 * query string, como exige el proveedor), el token, el payload en claro ni el teléfono completo.
 */
@Slf4j
@Component
public class PuntoRedClient {

    static final String CODIGO_EXITO = "01";
    static final String CODIGO_TIMEOUT_PROVEEDOR = "82";
    static final String CODIGO_CONFIRM_NO_ENCONTRADA = "03";

    private static final String HEADER_API_KEY = "X-API-Key";

    private final RestClient restClient;
    private final PuntoRedProperties properties;
    private final AesEncryptionService aesEncryptionService;
    private final PuntoRedXmlParser xmlParser;
    private final ObjectMapper objectMapper;

    // Cache thread-safe en memoria para el token de autorización
    private final AtomicReference<TokenInfo> cachedTokenRef = new AtomicReference<>();
    private final Object tokenLock = new Object();

    public PuntoRedClient(
            @Qualifier("puntoRedRestClient") RestClient restClient,
            PuntoRedProperties properties,
            AesEncryptionService aesEncryptionService,
            PuntoRedXmlParser xmlParser,
            ObjectMapper objectMapper) {
        this.restClient = restClient;
        this.properties = properties;
        this.aesEncryptionService = aesEncryptionService;
        this.xmlParser = xmlParser;
        this.objectMapper = objectMapper;
    }

    /**
     * Obtiene el token de autenticación (JWT) de PuntoRed invocando POST {baseUrl}{authPath}.
     * Las credenciales viajan estrictamente como query params.
     * Si el token es un JWT válido, se inspecciona el claim 'exp' para renovarlo antes de su expiración.
     * Si no tiene 'exp', se mantendrá en caché hasta recibir un HTTP 401/403.
     *
     * @return Token de autorización válido
     * @throws PuntoRedAutenticacionException Si PuntoRed rechaza la autenticación o success=false
     */
    public String obtenerToken() {
        TokenInfo actual = cachedTokenRef.get();
        if (actual != null && actual.isValid()) {
            return actual.token();
        }

        synchronized (tokenLock) {
            // Doble comprobación bajo bloqueo para evitar peticiones concurrentes duplicadas
            actual = cachedTokenRef.get();
            if (actual != null && actual.isValid()) {
                return actual.token();
            }

            long inicio = System.currentTimeMillis();
            log.info("Iniciando solicitud de autenticación ante PuntoRed [idDistribuidor: {}, codigoDispositivo: {}]",
                    properties.getIdDistribuidor(), properties.getCodigoDispositivo());

            try {
                String authPath = properties.getAuthPath() != null ? properties.getAuthPath() : "/sistema/app/jwt-gp/authenticate/";

                String responseBody = restClient.post()
                        .uri(uriBuilder -> uriBuilder
                                .path(authPath)
                                .queryParam("idDistribuidor", properties.getIdDistribuidor())
                                .queryParam("codigoDispositivo", properties.getCodigoDispositivo())
                                .queryParam("password", properties.getPassword())
                                .build())
                        .headers(this::agregarApiKey)
                        .accept(MediaType.APPLICATION_JSON)
                        .retrieve()
                        .body(String.class);

                long duracion = System.currentTimeMillis() - inicio;

                if (responseBody == null || responseBody.isBlank()) {
                    throw new PuntoRedAutenticacionException("PuntoRed devolvió una respuesta de autenticación vacía", "AUTH_EMPTY", null);
                }

                JsonNode json = objectMapper.readTree(responseBody);
                boolean success = json.has("success") && json.get("success").asBoolean();
                String token = json.has("token") ? json.get("token").asText() : null;

                if (!success || token == null || token.isBlank()) {
                    String mensajeError = json.has("message") ? json.get("message").asText() : "Credenciales no autorizadas";
                    log.warn("Autenticación rechazada por PuntoRed. Duración: {} ms", duracion);
                    throw new PuntoRedAutenticacionException("Autenticación rechazada por PuntoRed: " + mensajeError, "AUTH_FAIL", mensajeError);
                }

                Instant expiresAt = extraerExpiracionJwt(token);
                cachedTokenRef.set(new TokenInfo(token, expiresAt));

                log.info("Autenticación exitosa con PuntoRed. Token almacenado en caché. Duración: {} ms", duracion);
                return token;

            } catch (PuntoRedAutenticacionException e) {
                throw e;
            } catch (ResourceAccessException e) {
                log.error("Timeout de conexión al autenticar con PuntoRed. Duración: {} ms", System.currentTimeMillis() - inicio);
                throw new PuntoRedTimeoutException("Timeout de red al conectar con el servicio de autenticación de PuntoRed", e);
            } catch (Exception e) {
                // Sin e.getMessage(): las excepciones de RestClient incluyen la URI con la contraseña
                log.error("Falla al procesar la autenticación de PuntoRed [causa: {}]. Duración: {} ms",
                        e.getClass().getSimpleName(), System.currentTimeMillis() - inicio);
                throw new PuntoRedIntegracionException("Error al autenticar con la API de PuntoRed (" + e.getClass().getSimpleName() + ")");
            }
        }
    }

    /**
     * Consulta el catálogo de productos y servicios autorizados (getProductList.do).
     * Aplica renovación automática de token si se recibe un 401/403 (reintento único).
     */
    public PuntoRedProductosResponse getProductList() {
        return ejecutarConReintentoAuth(this::getProductListInternal);
    }

    private PuntoRedProductosResponse getProductListInternal(String token) {
        long inicio = System.currentTimeMillis();
        log.info("Consultando catálogo de productos en PuntoRed (getProductList.do)");

        try {
            String xmlResponse = restClient.get()
                    .uri("/sistema/service/getProductList.do")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .headers(this::agregarApiKey)
                    .accept(MediaType.APPLICATION_XML, MediaType.TEXT_XML)
                    .retrieve()
                    .body(String.class);

            long duracion = System.currentTimeMillis() - inicio;
            PuntoRedProductosResponse response = xmlParser.parseProductos(xmlResponse);

            String codigoRespuesta = response.getMensaje() != null ? response.getMensaje().getCodigo() : "N/A";
            int totalProductos = response.getProductos() != null ? response.getProductos().size() : 0;
            log.info("Catálogo de productos obtenido exitosamente. Código respuesta: {}, Total productos: {}, Duración: {} ms",
                    codigoRespuesta, totalProductos, duracion);

            return response;

        } catch (HttpClientErrorException.Unauthorized | HttpClientErrorException.Forbidden e) {
            throw e; // Capturado por ejecutarConReintentoAuth para reintento único
        } catch (ResourceAccessException e) {
            log.error("Timeout al consultar getProductList de PuntoRed. Duración: {} ms", System.currentTimeMillis() - inicio);
            throw new PuntoRedTimeoutException("Timeout al consultar catálogo de productos de PuntoRed", e);
        } catch (PuntoRedRespuestaInvalidaException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error al obtener catálogo de productos de PuntoRed [causa: {}]. Duración: {} ms",
                    e.getClass().getSimpleName(), System.currentTimeMillis() - inicio);
            throw new PuntoRedIntegracionException("Error al invocar getProductList.do de PuntoRed (" + e.getClass().getSimpleName() + ")", e);
        }
    }

    /**
     * Envía una transacción de recarga o pago de servicios a PuntoRed (sendTx.do).
     * <p>
     * Formato oficial: {@code application/x-www-form-urlencoded} con {@code random} (timestamp) y
     * {@code signed} (JSON del payload cifrado con AES-256-CBC en Base64).
     * <p>
     * Interpretación del CODIGO de respuesta:
     * <ul>
     *   <li>01 → aprobada (se retorna la respuesta).</li>
     *   <li>06 → {@link PuntoRedTransaccionDuplicadaException}; el llamador decide según el contexto.</li>
     *   <li>82 → {@link PuntoRedTimeoutException} con el ID_TX: resultado DESCONOCIDO, se resuelve con confirmTx.</li>
     *   <li>Cualquier otro → {@link PuntoRedRechazoException} (rechazo confirmado).</li>
     * </ul>
     *
     * @throws PuntoRedTimeoutException           timeout de red o CODIGO 82 (resultado DESCONOCIDO)
     * @throws PuntoRedRespuestaInvalidaException respuesta XML ilegible (resultado DESCONOCIDO)
     */
    public PuntoRedTxResponse sendTx(SendTxPayload payload) {
        if (payload == null) {
            throw new PuntoRedIntegracionException("El payload de la transacción no puede ser nulo");
        }
        payload.validar();

        // Se cifra una sola vez: un reintento por token vencido reenvía exactamente la misma trama
        MultiValueMap<String, String> formulario = new LinkedMultiValueMap<>();
        formulario.add("random", String.valueOf(System.currentTimeMillis()));
        formulario.add("signed", cifrar(payload));

        return ejecutarConReintentoAuth(token -> sendTxInternal(formulario, token, payload));
    }

    private String cifrar(Object payload) {
        try {
            return aesEncryptionService.encrypt(objectMapper.writeValueAsString(payload));
        } catch (Exception e) {
            throw new PuntoRedIntegracionException("Falla al cifrar el payload de la transacción con AES (" + e.getClass().getSimpleName() + ")", e);
        }
    }

    private PuntoRedTxResponse sendTxInternal(MultiValueMap<String, String> formulario, String token, SendTxPayload payload) {
        String upc = payload.getUpc();
        long inicio = System.currentTimeMillis();
        log.info("Enviando transacción a PuntoRed (sendTx.do) [idProducto: {}, upc: {}, telefono: {}]",
                payload.getIdProducto(), upc, payload.getTelefonoEnmascarado());

        PuntoRedTxResponse txResponse;
        try {
            String xmlResponse = restClient.post()
                    .uri("/sistema/service/sendTx.do")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .headers(this::agregarApiKey)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .accept(MediaType.APPLICATION_XML, MediaType.TEXT_XML)
                    .body(formulario)
                    .retrieve()
                    .body(String.class);

            txResponse = xmlParser.parseSendTx(xmlResponse);

        } catch (HttpClientErrorException.Unauthorized | HttpClientErrorException.Forbidden e) {
            throw e; // Capturado por ejecutarConReintentoAuth para reintento único
        } catch (PuntoRedRespuestaInvalidaException e) {
            throw e;
        } catch (Exception e) {
            long duracion = System.currentTimeMillis() - inicio;
            if (esErrorTimeout(e)) {
                log.error("TIMEOUT / Error de red al enviar sendTx a PuntoRed [upc: {}, duracion: {} ms]. Resultado DESCONOCIDO.",
                        upc, duracion);
                throw new PuntoRedTimeoutException(
                        "Tiempo de espera agotado al comunicar la transacción sendTx (UPC: " + upc + "). Resultado DESCONOCIDO.", e);
            }
            log.error("Falla no controlada en sendTx [upc: {}, causa: {}, duracion: {} ms]", upc, e.getClass().getSimpleName(), duracion);
            throw new PuntoRedIntegracionException("Error inesperado al ejecutar sendTx.do (" + e.getClass().getSimpleName() + ")", e);
        }

        String codigo = txResponse.getMensaje() != null ? txResponse.getMensaje().getCodigo() : null;
        String texto = txResponse.getMensaje() != null ? txResponse.getMensaje().getTexto() : "SIN_DESCRIPCION";
        log.info("Respuesta de sendTx recibida de PuntoRed [upc: {}, codigo: {}, duracion: {} ms]",
                upc, codigo, System.currentTimeMillis() - inicio);

        if (CODIGO_EXITO.equals(codigo)) {
            return txResponse;
        }
        if (PuntoRedTransaccionDuplicadaException.CODIGO.equals(codigo)) {
            log.warn("PuntoRed reporta transacción aplicada anteriormente [upc: {}]", upc);
            throw new PuntoRedTransaccionDuplicadaException(texto);
        }
        if (CODIGO_TIMEOUT_PROVEEDOR.equals(codigo)) {
            String idTx = txResponse.getIdTx() != null ? txResponse.getIdTx() : txResponse.getMensaje().getIdTx();
            log.error("PuntoRed agotó su tiempo de espera (CODIGO 82) [upc: {}, idTx: {}]. Resultado DESCONOCIDO.", upc, idTx);
            throw new PuntoRedTimeoutException("PuntoRed agotó su tiempo de espera (UPC: " + upc + "). Resultado DESCONOCIDO.",
                    codigo, texto, idTx);
        }
        if (codigo == null || codigo.isBlank()) {
            throw new PuntoRedRespuestaInvalidaException("La respuesta de sendTx no contiene CODIGO (UPC: " + upc + ")");
        }
        log.warn("Transacción RECHAZADA por PuntoRed [upc: {}, codigo: {}, texto: {}]", upc, codigo, texto);
        throw new PuntoRedRechazoException(codigo, texto);
    }

    /**
     * Consulta el estado de una transacción cuyo resultado se desconoce (confirmTx.do).
     * Solo es válido durante 24 h posteriores al sendTx y requiere el ID_TX que PuntoRed devolvió.
     *
     * @param idTx ID_TX devuelto por PuntoRed (p. ej. junto con el CODIGO 82)
     * @throws PuntoRedTimeoutException           si la consulta misma no obtiene respuesta
     * @throws PuntoRedRespuestaInvalidaException si la respuesta es ilegible
     */
    public ConfirmacionTx confirmTx(String idTx) {
        if (idTx == null || idTx.isBlank()) {
            throw new PuntoRedIntegracionException("confirmTx requiere el ID_TX de la transacción");
        }
        MultiValueMap<String, String> formulario = new LinkedMultiValueMap<>();
        formulario.add("idDistribuidor", properties.getIdDistribuidor());
        formulario.add("idTransaccion", idTx.trim());

        return ejecutarConReintentoAuth(token -> confirmTxInternal(formulario, token, idTx.trim()));
    }

    private ConfirmacionTx confirmTxInternal(MultiValueMap<String, String> formulario, String token, String idTx) {
        long inicio = System.currentTimeMillis();
        PuntoRedTxResponse respuesta;
        try {
            String xmlResponse = restClient.post()
                    .uri("/sistema/service/confirmTx.do")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .headers(this::agregarApiKey)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .accept(MediaType.APPLICATION_XML, MediaType.TEXT_XML)
                    .body(formulario)
                    .retrieve()
                    .body(String.class);
            respuesta = xmlParser.parseSendTx(xmlResponse);
        } catch (HttpClientErrorException.Unauthorized | HttpClientErrorException.Forbidden e) {
            throw e;
        } catch (PuntoRedRespuestaInvalidaException e) {
            throw e;
        } catch (Exception e) {
            if (esErrorTimeout(e)) {
                throw new PuntoRedTimeoutException("Tiempo de espera agotado en confirmTx (idTx: " + idTx + ")", e);
            }
            log.error("Falla no controlada en confirmTx [idTx: {}, causa: {}]", idTx, e.getClass().getSimpleName());
            throw new PuntoRedIntegracionException("Error inesperado al ejecutar confirmTx.do (" + e.getClass().getSimpleName() + ")", e);
        }

        String codigo = respuesta.getMensaje() != null ? respuesta.getMensaje().getCodigo() : null;
        if (respuesta.getIdTx() == null && respuesta.getMensaje() != null) {
            respuesta.setIdTx(respuesta.getMensaje().getIdTx() != null ? respuesta.getMensaje().getIdTx() : idTx);
        }

        ConfirmacionTx.Estado estado = switch (codigo == null ? "" : codigo) {
            case CODIGO_EXITO -> ConfirmacionTx.Estado.APLICADA;
            case CODIGO_CONFIRM_NO_ENCONTRADA -> ConfirmacionTx.Estado.NO_ENCONTRADA;
            // 04 (pendiente) y cualquier código no documentado: no hay certeza, no se mueve el saldo
            default -> ConfirmacionTx.Estado.PENDIENTE;
        };
        log.info("Respuesta de confirmTx [idTx: {}, codigo: {}, estado: {}, duracion: {} ms]",
                idTx, codigo, estado, System.currentTimeMillis() - inicio);
        return new ConfirmacionTx(estado, respuesta);
    }

    /**
     * Ejecuta la llamada con un token válido y reintenta una única vez si PuntoRed responde 401 o 403
     * (la documentación indica 403 para token vencido). El rechazo de autenticación ocurre antes de procesar
     * la transacción, y el UPC repetido protege adicionalmente contra un doble cargo.
     */
    private <T> T ejecutarConReintentoAuth(TokenConsumer<T> accion) {
        String token = obtenerToken();
        try {
            return accion.apply(token);
        } catch (HttpClientErrorException.Unauthorized | HttpClientErrorException.Forbidden e) {
            log.warn("PuntoRed respondió HTTP {} (token inválido o vencido). Renovando token y reintentando una única vez...",
                    e.getStatusCode().value());
            invalidarToken();
            String nuevoToken = obtenerToken();
            try {
                return accion.apply(nuevoToken);
            } catch (HttpClientErrorException.Unauthorized | HttpClientErrorException.Forbidden ex) {
                log.error("El reintento con nuevo token también fue rechazado con HTTP {}", ex.getStatusCode().value());
                throw new PuntoRedAutenticacionException("Autenticación rechazada por PuntoRed tras reintento",
                        String.valueOf(ex.getStatusCode().value()), "Unauthorized");
            }
        }
    }

    private void agregarApiKey(HttpHeaders headers) {
        if (properties.getApiKey() != null && !properties.getApiKey().isBlank()) {
            headers.set(HEADER_API_KEY, properties.getApiKey().trim());
        }
    }

    public void invalidarToken() {
        cachedTokenRef.set(null);
    }

    private Instant extraerExpiracionJwt(String token) {
        if (token == null) {
            return null;
        }
        String[] partes = token.split("\\.");
        if (partes.length < 2) {
            return null;
        }
        try {
            byte[] payloadBytes = Base64.getUrlDecoder().decode(partes[1]);
            JsonNode payloadJson = objectMapper.readTree(payloadBytes);
            if (payloadJson.has("exp")) {
                return Instant.ofEpochSecond(payloadJson.get("exp").asLong());
            }
        } catch (Exception e) {
            log.debug("No fue posible extraer el claim 'exp' del JWT [causa: {}]", e.getClass().getSimpleName());
        }
        return null;
    }

    @FunctionalInterface
    private interface TokenConsumer<T> {
        T apply(String token);
    }

    private record TokenInfo(String token, Instant expiresAt) {
        public boolean isValid() {
            if (token == null || token.isBlank()) {
                return false;
            }
            if (expiresAt == null) {
                return true; // No tiene expiración fija, se invalidará ante 401/403
            }
            // Renovar si faltan menos de 60 segundos para vencer
            return Instant.now().isBefore(expiresAt.minusSeconds(60));
        }
    }

    private boolean esErrorTimeout(Throwable t) {
        if (t == null) {
            return false;
        }
        if (t instanceof ResourceAccessException
                || t instanceof java.net.SocketTimeoutException
                || t instanceof java.util.concurrent.TimeoutException
                || t instanceof org.apache.hc.core5.http.ConnectionClosedException) {
            return true;
        }
        String msg = t.getMessage() != null ? t.getMessage().toLowerCase() : "";
        if (msg.contains("timeout") || msg.contains("timed out") || msg.contains("time out")) {
            return true;
        }
        return esErrorTimeout(t.getCause());
    }
}
