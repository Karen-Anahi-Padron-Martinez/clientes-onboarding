package com.kapm.onboarding_clientes.client.puntored.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.*;

/**
 * Modelo para un producto individual devuelto en el catálogo de servicios de PuntoRed.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class PuntoRedProducto {

    @JacksonXmlProperty(localName = "producto")
    @JsonAlias({"PRODUCTO", "Producto"})
    private String producto;

    @JacksonXmlProperty(localName = "servicio")
    @JsonAlias({"SERVICIO", "Servicio"})
    private String servicio;

    @JacksonXmlProperty(localName = "idServicio")
    @JsonAlias({"ID_SERVICIO", "IdServicio", "id_servicio"})
    private Integer idServicio;

    @JacksonXmlProperty(localName = "idProducto")
    @JsonAlias({"ID_PRODUCTO", "IdProducto", "id_producto"})
    private Integer idProducto;

    @JacksonXmlProperty(localName = "idCatTipoServicio")
    @JsonAlias({"ID_CAT_TIPO_SERVICIO", "IdCatTipoServicio", "id_cat_tipo_servicio"})
    private Integer idCatTipoServicio;

    @JacksonXmlProperty(localName = "tipoFront")
    @JsonAlias({"TIPO_FRONT", "TipoFront", "tipo_front"})
    private Integer tipoFront;
}
