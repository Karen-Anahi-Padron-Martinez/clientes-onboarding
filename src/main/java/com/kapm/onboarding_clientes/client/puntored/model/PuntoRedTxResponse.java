package com.kapm.onboarding_clientes.client.puntored.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;

/**
 * Modelo para la respuesta XML de una transacción procesada mediante sendTx.do de PuntoRed.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
@JacksonXmlRootElement(localName = "RESPUESTA")
public class PuntoRedTxResponse {

    public static final DateTimeFormatter FECHA_FORMATTER_EN =
            DateTimeFormatter.ofPattern("d/MMM/yyyy HH:mm:ss", Locale.ENGLISH);

    @JacksonXmlProperty(localName = "ID_TX")
    @JsonAlias({"idTx", "id_tx", "IDTX", "IdTx"})
    private String idTx;

    @JacksonXmlProperty(localName = "NUM_AUTORIZACION")
    @JsonAlias({"numAutorizacion", "num_autorizacion", "autorizacion", "AUTORIZACION"})
    private String numAutorizacion;

    @JacksonXmlProperty(localName = "SALDO")
    @JsonAlias({"saldo", "Saldo"})
    private BigDecimal saldo;

    @JacksonXmlProperty(localName = "COMISION")
    @JsonAlias({"comision", "Comision"})
    private BigDecimal comision;

    @JacksonXmlProperty(localName = "SALDO_F")
    @JsonAlias({"saldoF", "saldo_f", "SaldoF"})
    private String saldoF;

    @JacksonXmlProperty(localName = "COMISION_F")
    @JsonAlias({"comisionF", "comision_f", "ComisionF"})
    private String comisionF;

    @JacksonXmlProperty(localName = "FECHA")
    @JsonAlias({"fecha", "Fecha"})
    private String fecha;

    @JacksonXmlProperty(localName = "MONTO")
    @JsonAlias({"monto", "Monto"})
    private BigDecimal monto;

    @JacksonXmlProperty(localName = "MENSAJE")
    @JsonAlias({"mensaje", "Mensaje"})
    private PuntoRedMensaje mensaje;

    /**
     * Parsea la fecha de respuesta ('25/Feb/2022 18:15:25') utilizando estrictamente Locale.ENGLISH.
     * Nota: Si PuntoRed envía meses en español como 'Dic' (vs 'Dec'), 'Ene' (vs 'Jan'), 'Abr' (vs 'Apr') o 'Ago' (vs 'Aug'),
     * este parser arrojará DateTimeParseException bajo Locale.ENGLISH.
     *
     * @return LocalDateTime parseado o null si la fecha está ausente
     * @throws DateTimeParseException Si el formato o el mes no coinciden con Locale.ENGLISH
     */
    public LocalDateTime getFechaParsed() {
        if (fecha == null || fecha.isBlank()) {
            return null;
        }
        return LocalDateTime.parse(fecha.trim(), FECHA_FORMATTER_EN);
    }
}
