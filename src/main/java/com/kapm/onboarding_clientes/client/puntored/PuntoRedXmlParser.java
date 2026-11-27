package com.kapm.onboarding_clientes.client.puntored;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.kapm.onboarding_clientes.client.puntored.model.PuntoRedMensaje;
import com.kapm.onboarding_clientes.client.puntored.model.PuntoRedProductosResponse;
import com.kapm.onboarding_clientes.client.puntored.model.PuntoRedTxResponse;
import com.kapm.onboarding_clientes.exception.PuntoRedRespuestaInvalidaException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Optional;

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

        if (esRaizMensaje(xml.trim(), "<PRODUCTO")) {
            Optional<PuntoRedMensaje> mensaje = leerMensaje(xml);
            if (mensaje.isPresent()) {
                return productosConMensaje(mensaje.get());
            }
        }

        Optional<PuntoRedProductosResponse> response = leer(xml, PuntoRedProductosResponse.class)
                .filter(r -> r.getMensaje() != null || !CollectionUtils.isEmpty(r.getProductos()));
        if (response.isPresent()) {
            return response.get();
        }

        return leerMensaje(xml)
                .map(this::productosConMensaje)
                .orElseThrow(() -> new PuntoRedRespuestaInvalidaException(
                        "La respuesta XML de productos es inesperada o no tiene un formato válido"));
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

        // Con raíz MENSAJE se lee primero como mensaje: en confirmTx el ID_TX viaja dentro de ese bloque
        if (esRaizMensaje(xml.trim(), "<ID_TX")) {
            Optional<PuntoRedMensaje> mensaje = leerMensaje(xml);
            if (mensaje.isPresent()) {
                return txConMensaje(mensaje.get());
            }
        }

        Optional<PuntoRedTxResponse> txResponse = leer(xml, PuntoRedTxResponse.class)
                .filter(tx -> tx.getIdTx() != null || tx.getMensaje() != null || tx.getNumAutorizacion() != null);
        if (txResponse.isPresent()) {
            return txResponse.get();
        }

        return leerMensaje(xml)
                .map(this::txConMensaje)
                .orElseThrow(() -> new PuntoRedRespuestaInvalidaException(
                        "La respuesta XML de transacción sendTx es inválida, inesperada o no contiene campos reconocibles"));
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

    /** La raíz es MENSAJE, o un documento con declaración XML que trae MENSAJE pero no el nodo propio de la operación. */
    private static boolean esRaizMensaje(String xml, String nodoOperacion) {
        return xml.startsWith("<MENSAJE")
                || (xml.startsWith("<?xml") && xml.contains("<MENSAJE") && !xml.contains(nodoOperacion));
    }

    private Optional<PuntoRedMensaje> leerMensaje(String xml) {
        return leer(xml, PuntoRedMensaje.class).filter(m -> m.getCodigo() != null);
    }

    private <T> Optional<T> leer(String xml, Class<T> tipo) {
        try {
            return Optional.ofNullable(xmlMapper.readValue(xml, tipo));
        } catch (Exception e) {
            log.debug("No se pudo deserializar la respuesta como {}: {}", tipo.getSimpleName(), e.getMessage());
            return Optional.empty();
        }
    }

    private PuntoRedProductosResponse productosConMensaje(PuntoRedMensaje mensaje) {
        return PuntoRedProductosResponse.builder().mensaje(mensaje).productos(List.of()).build();
    }

    private PuntoRedTxResponse txConMensaje(PuntoRedMensaje mensaje) {
        return PuntoRedTxResponse.builder().mensaje(mensaje).build();
    }

    private void validarXmlNoVacio(String xml, String operacion) {
        if (xml == null || xml.isBlank()) {
            throw new PuntoRedRespuestaInvalidaException("La respuesta XML de PuntoRed para " + operacion + " está vacía o nula");
        }
    }
}
