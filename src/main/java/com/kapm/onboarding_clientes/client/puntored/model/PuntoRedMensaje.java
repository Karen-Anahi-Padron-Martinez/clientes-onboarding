package com.kapm.onboarding_clientes.client.puntored.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.*;

/**
 * Modelo para el bloque estándar MENSAJE de las respuestas XML de PuntoRed.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
@JacksonXmlRootElement(localName = "MENSAJE")
public class PuntoRedMensaje {

    @JacksonXmlProperty(localName = "CODIGO")
    @JsonAlias({"codigo", "Codigo", "CODIGO"})
    private String codigo;

    @JacksonXmlProperty(localName = "TEXTO")
    @JsonAlias({"texto", "Texto", "TEXTO", "mensaje", "MENSAJE"})
    private String texto;

    /** En confirmTx el ID_TX viaja dentro del bloque MENSAJE. */
    @JacksonXmlProperty(localName = "ID_TX")
    @JsonAlias({"idTx", "id_tx"})
    private String idTx;

    public PuntoRedMensaje(String codigo, String texto) {
        this.codigo = codigo;
        this.texto = texto;
    }

    /** Único código de éxito documentado por PuntoRed para sendTx y confirmTx. */
    public boolean isExitoso() {
        return "01".equals(codigo);
    }
}
