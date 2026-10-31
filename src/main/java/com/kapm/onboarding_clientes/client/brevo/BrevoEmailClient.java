package com.kapm.onboarding_clientes.client.brevo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kapm.onboarding_clientes.client.brevo.model.BrevoEmailRequest;
import com.kapm.onboarding_clientes.client.brevo.model.BrevoRecipient;
import com.kapm.onboarding_clientes.client.brevo.model.BrevoSender;
import com.kapm.onboarding_clientes.config.BrevoProperties;
import com.kapm.onboarding_clientes.exception.CorreoNoEnviadoException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.net.SocketTimeoutException;
import java.util.List;

/**
 * Cliente HTTP REST para el envío de correos transaccionales a través de la API oficial de Brevo.
 * <p>
 * Reglas de seguridad y privacidad:
 * <ul>
 *   <li>NUNCA registrar en logs la API Key de Brevo.</li>
 *   <li>NUNCA registrar en logs el cuerpo del correo (html), ya que contiene códigos de seguridad u OTPs.</li>
 *   <li>El correo del destinatario siempre debe registrarse enmascarado (ej. j***@dominio.com).</li>
 * </ul>
 */
@Slf4j
@Component
public class BrevoEmailClient {

    private static final String API_KEY_HEADER = "api-key";
    private static final String EMAIL_ENDPOINT = "/v3/smtp/email";

    private final RestClient restClient;
    private final BrevoProperties properties;
    private final ObjectMapper objectMapper;

    @org.springframework.beans.factory.annotation.Autowired
    public BrevoEmailClient(
            @Qualifier("brevoRestClient") RestClient restClient,
            BrevoProperties properties,
            ObjectMapper objectMapper) {
        this.restClient = restClient;
        this.properties = properties;
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
    }

    public BrevoEmailClient(RestClient restClient, BrevoProperties properties) {
        this(restClient, properties, new ObjectMapper());
    }

    /**
     * Envía un correo electrónico transaccional a través de la API REST de Brevo.
     *
     * @param destinatario Dirección de correo del receptor
     * @param asunto       Asunto del correo
     * @param html         Contenido HTML del mensaje (puede incluir códigos de seguridad)
     * @return messageId generado por Brevo para auditoría y trazabilidad
     * @throws CorreoNoEnviadoException Si Brevo rechaza la solicitud, expira el timeout o falla la conexión
     */
    public String enviarCorreo(String destinatario, String asunto, String html) {
        validarParametros(destinatario, asunto, html);

        String correoEnmascarado = enmascararCorreo(destinatario);
        log.info("Enviando correo transaccional a {} con asunto '{}'", correoEnmascarado, asunto);

        BrevoEmailRequest requestPayload = BrevoEmailRequest.builder()
                .sender(BrevoSender.builder()
                        .name(properties.getSenderName() != null ? properties.getSenderName().trim() : "Banco Digital")
                        .email(properties.getSenderEmail().trim())
                        .build())
                .to(List.of(BrevoRecipient.builder().email(destinatario.trim()).build()))
                .subject(asunto.trim())
                .htmlContent(html)
                .build();

        long inicio = System.currentTimeMillis();
        try {
            String responseBody = restClient.post()
                    .uri(EMAIL_ENDPOINT)
                    .header(API_KEY_HEADER, properties.getApiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(requestPayload)
                    .retrieve()
                    .body(String.class);

            long duracion = System.currentTimeMillis() - inicio;
            String messageId = extraerMessageId(responseBody);

            log.info("Correo transaccional enviado exitosamente a {} [messageId: {}, duracion: {} ms]",
                    correoEnmascarado, messageId != null ? messageId : "N/A", duracion);

            return messageId;

        } catch (HttpClientErrorException.Unauthorized ex) {
            long duracion = System.currentTimeMillis() - inicio;
            log.error("Autenticación fallida con Brevo (HTTP 401) al enviar a {}. Duración: {} ms",
                    correoEnmascarado, duracion);
            throw new CorreoNoEnviadoException("Error de autenticación con Brevo (HTTP 401): API key inválida o no autorizada", ex);

        } catch (RestClientResponseException ex) {
            long duracion = System.currentTimeMillis() - inicio;
            HttpStatusCode status = ex.getStatusCode();
            log.error("Brevo respondió con error HTTP {} al enviar correo a {}. Duración: {} ms",
                    status.value(), correoEnmascarado, duracion);

            if (status.value() == 401) {
                throw new CorreoNoEnviadoException("Error de autenticación con Brevo (HTTP 401): API key inválida o no autorizada", ex);
            }
            throw new CorreoNoEnviadoException("Error del servicio de correos Brevo (HTTP " + status.value() + "): " + ex.getStatusText(), ex);

        } catch (ResourceAccessException ex) {
            long duracion = System.currentTimeMillis() - inicio;
            log.error("Timeout de red al conectar con Brevo para enviar a {}. Duración: {} ms",
                    correoEnmascarado, duracion);
            throw new CorreoNoEnviadoException("Tiempo de espera agotado al conectar con el servicio de correos Brevo", ex);

        } catch (Exception ex) {
            long duracion = System.currentTimeMillis() - inicio;
            log.error("Falla inesperada al enviar correo transaccional a {}. Duración: {} ms: {}",
                    correoEnmascarado, duracion, ex.getMessage());

            if (esTimeout(ex)) {
                throw new CorreoNoEnviadoException("Tiempo de espera agotado al comunicar con el proveedor Brevo", ex);
            }
            throw new CorreoNoEnviadoException("Error al enviar el correo electrónico con Brevo: " + ex.getMessage(), ex);
        }
    }

    /**
     * Enmascara una dirección de correo para registros de auditoría y bitácoras seguras.
     * Ejemplo: "juan.perez@dominio.com" -> "j***@dominio.com".
     *
     * @param email Correo electrónico a enmascarar
     * @return Cadena con el formato 'j***@dominio.com'
     */
    public static String enmascararCorreo(String email) {
        if (email == null || email.isBlank()) {
            return "***";
        }
        String trimmed = email.trim();
        int atIndex = trimmed.indexOf('@');
        if (atIndex <= 0) {
            return "***";
        }
        String usuario = trimmed.substring(0, atIndex);
        String dominio = trimmed.substring(atIndex + 1);

        char primerCaracter = usuario.charAt(0);
        return primerCaracter + "***@" + dominio;
    }

    // =========================================================================
    // MÉTODOS PRIVADOS AUXILIARES
    // =========================================================================

    private void validarParametros(String destinatario, String asunto, String html) {
        if (destinatario == null || destinatario.isBlank()) {
            throw new CorreoNoEnviadoException("El destinatario del correo es obligatorio");
        }
        if (!destinatario.contains("@") || destinatario.trim().length() < 5) {
            throw new CorreoNoEnviadoException("El formato del correo del destinatario es inválido");
        }
        if (asunto == null || asunto.isBlank()) {
            throw new CorreoNoEnviadoException("El asunto del correo es obligatorio");
        }
        if (html == null || html.isBlank()) {
            throw new CorreoNoEnviadoException("El contenido HTML del correo es obligatorio");
        }
        if (properties.getApiKey() == null || properties.getApiKey().isBlank()) {
            throw new CorreoNoEnviadoException("La API key de Brevo no se encuentra configurada");
        }
        if (properties.getSenderEmail() == null || properties.getSenderEmail().isBlank()) {
            throw new CorreoNoEnviadoException("El correo del remitente de Brevo no se encuentra configurado");
        }
    }

    private String extraerMessageId(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return null;
        }
        try {
            JsonNode jsonNode = objectMapper.readTree(responseBody);
            if (jsonNode.has("messageId")) {
                return jsonNode.get("messageId").asText();
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private boolean esTimeout(Throwable throwable) {
        Throwable actual = throwable;
        while (actual != null) {
            if (actual instanceof SocketTimeoutException) {
                return true;
            }
            String msg = actual.getMessage();
            if (msg != null && (msg.toLowerCase().contains("timeout") || msg.toLowerCase().contains("timed out"))) {
                return true;
            }
            actual = actual.getCause();
        }
        return false;
    }
}
