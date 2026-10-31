package com.kapm.onboarding_clientes.client.puntored.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Modelo contenedor para la respuesta XML de getProductList.do de PuntoRed.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
@JacksonXmlRootElement(localName = "PRODUCTOS")
public class PuntoRedProductosResponse {

    @JacksonXmlProperty(localName = "MENSAJE")
    @JsonAlias({"mensaje", "Mensaje"})
    private PuntoRedMensaje mensaje;

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "PRODUCTO")
    @JsonAlias({"producto", "Producto", "PRODUCTOS", "productos"})
    @Builder.Default
    private List<PuntoRedProducto> productos = new ArrayList<>();
}
