package com.kapm.onboarding_clientes.client.puntored;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.kapm.onboarding_clientes.client.puntored.model.PuntoRedMensaje;
import com.kapm.onboarding_clientes.client.puntored.model.PuntoRedProducto;
import com.kapm.onboarding_clientes.client.puntored.model.PuntoRedProductosResponse;
import com.kapm.onboarding_clientes.client.puntored.model.PuntoRedTxResponse;
import com.kapm.onboarding_clientes.exception.PuntoRedRespuestaInvalidaException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Parser XML especializado exclusivamente para procesar las respuestas XML recibidas
 * desde la API de PuntoRed (GestoPago) para getProductList y sendTx.
 */
@Slf4j
@Component
public class PuntoRedXmlParser {

    private final XmlMapper xmlMapper;

    public PuntoRedXmlParser() {
        this.xmlMapper = new XmlMapper();
        this.xmlMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.xmlMapper.configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true);
    }

    /**
     * Parsea la respuesta XML de getProductList.do a un objeto PuntoRedProductosResponse.
     *
     * @param xml Contenido XML recibido del proveedor
     * @return Objeto PuntoRedProductosResponse con el listado de productos y/o mensaje
     * @throws PuntoRedRespuestaInvalidaException Si el XML está vacío o no se puede interpretar
     */
    public PuntoRedProductosResponse parseProductos(String xml) {
        validarXmlNoVacio(xml, "getProductList");

        String trimmed = xml.trim();
        if (trimmed.startsWith("<MENSAJE") || trimmed.startsWith("<?xml") && trimmed.contains("<MENSAJE") && !trimmed.contains("<PRODUCTO")) {
            try {
                PuntoRedMensaje mensaje = xmlMapper.readValue(xml, PuntoRedMensaje.class);
                if (mensaje != null && mensaje.getCodigo() != null) {
                    return PuntoRedProductosResponse.builder()
                            .mensaje(mensaje)
                            .productos(List.of())
                            .build();
                }
            } catch (Exception e) {
                log.debug("Intento inicial como nodo raíz MENSAJE falló: {}", e.getMessage());
            }
        }

        try {
            PuntoRedProductosResponse response = xmlMapper.readValue(xml, PuntoRedProductosResponse.class);
            if (response != null && (response.getMensaje() != null || (response.getProductos() != null && !response.getProductos().isEmpty()))) {
                return response;
            }
        } catch (Exception e) {
            log.debug("No se pudo deserializar como PRODUCTOS, intentando como MENSAJE de error: {}", e.getMessage());
        }

        try {
            PuntoRedMensaje mensaje = xmlMapper.readValue(xml, PuntoRedMensaje.class);
            if (mensaje != null && mensaje.getCodigo() != null) {
                return PuntoRedProductosResponse.builder()
                        .mensaje(mensaje)
                        .productos(List.of())
                        .build();
            }
        } catch (Exception ex) {
            log.debug("Fallo al intentar deserializar como MENSAJE: {}", ex.getMessage());
        }

        throw new PuntoRedRespuestaInvalidaException("La respuesta XML de productos es inesperada o no tiene un formato válido");
    }

    /**
     * Parsea la respuesta XML de sendTx.do a un objeto PuntoRedTxResponse.
     *
     * @param xml Contenido XML recibido del proveedor
     * @return Objeto PuntoRedTxResponse con los datos financieros y de autorización
     * @throws PuntoRedRespuestaInvalidaException Si el XML está vacío o no se puede interpretar
     */
    public PuntoRedTxResponse parseSendTx(String xml) {
        validarXmlNoVacio(xml, "sendTx");

        String trimmed = xml.trim();
        if (trimmed.startsWith("<MENSAJE") || trimmed.startsWith("<?xml") && trimmed.contains("<MENSAJE") && !trimmed.contains("<ID_TX")) {
            try {
                PuntoRedMensaje mensaje = xmlMapper.readValue(xml, PuntoRedMensaje.class);
                if (mensaje != null && mensaje.getCodigo() != null) {
                    return PuntoRedTxResponse.builder()
                            .mensaje(mensaje)
                            .build();
                }
            } catch (Exception e) {
                log.debug("Intento de sendTx como nodo raíz MENSAJE falló: {}", e.getMessage());
            }
        }

        try {
            PuntoRedTxResponse txResponse = xmlMapper.readValue(xml, PuntoRedTxResponse.class);
            if (txResponse != null && (txResponse.getIdTx() != null || txResponse.getMensaje() != null || txResponse.getNumAutorizacion() != null)) {
                return txResponse;
            }
        } catch (Exception e) {
            log.debug("No se pudo deserializar directamente a PuntoRedTxResponse: {}", e.getMessage());
        }

        try {
            PuntoRedMensaje mensaje = xmlMapper.readValue(xml, PuntoRedMensaje.class);
            if (mensaje != null && mensaje.getCodigo() != null) {
                return PuntoRedTxResponse.builder()
                        .mensaje(mensaje)
                        .build();
            }
        } catch (Exception ex) {
            log.debug("Fallo al intentar interpretar respuesta de sendTx como MENSAJE: {}", ex.getMessage());
        }

        throw new PuntoRedRespuestaInvalidaException("La respuesta XML de transacción sendTx es inválida, inesperada o no contiene campos reconocibles");
    }

    /**
     * Parsea un nodo genérico MENSAJE.
     */
    public PuntoRedMensaje parseMensaje(String xml) {
        validarXmlNoVacio(xml, "MENSAJE");
        try {
            return xmlMapper.readValue(xml, PuntoRedMensaje.class);
        } catch (Exception e) {
            throw new PuntoRedRespuestaInvalidaException("No se pudo parsear el bloque MENSAJE del XML de PuntoRed", e);
        }
    }

    private void validarXmlNoVacio(String xml, String operacion) {
        if (xml == null || xml.isBlank()) {
            throw new PuntoRedRespuestaInvalidaException("La respuesta XML de PuntoRed para " + operacion + " está vacía o nula");
        }
    }
}
