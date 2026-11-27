package com.kapm.onboarding_clientes.client.puntored;

import com.kapm.onboarding_clientes.client.puntored.model.PuntoRedProductosResponse;
import com.kapm.onboarding_clientes.client.puntored.model.PuntoRedTxResponse;
import com.kapm.onboarding_clientes.exception.PuntoRedRespuestaInvalidaException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("PuntoRedXmlParser: formatos de raíz, documentos incompletos y XML ilegible")
class PuntoRedXmlParserCasosTest {

    private static final String DECL = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
    private final PuntoRedXmlParser parser = new PuntoRedXmlParser();

    @Test
    @DisplayName("Productos: MENSAJE con declaración XML, sin productos")
    void productosMensajeConDeclaracion() {
        PuntoRedProductosResponse r = parser.parseProductos(DECL + "<MENSAJE><CODIGO>03</CODIGO><TEXTO>Sin datos</TEXTO></MENSAJE>");
        assertThat(r.getMensaje().getCodigo()).isEqualTo("03");
        assertThat(r.getProductos()).isEmpty();
    }

    @Test
    @DisplayName("Productos: con declaración y nodo PRODUCTO se lee como catálogo")
    void productosConDeclaracionYProducto() {
        String xml = DECL + "<PRODUCTOS><MENSAJE><CODIGO>01</CODIGO></MENSAJE>"
                + "<PRODUCTO><ID_PRODUCTO>55</ID_PRODUCTO></PRODUCTO></PRODUCTOS>";
        assertThat(parser.parseProductos(xml).getMensaje().getCodigo()).isEqualTo("01");
        assertThat(parser.parseProductos(DECL + "<PRODUCTOS><PRODUCTO><ID_PRODUCTO>7</ID_PRODUCTO></PRODUCTO></PRODUCTOS>").getProductos()).hasSize(1);
    }

    @Test
    @DisplayName("Productos: raíz desconocida con CODIGO se interpreta como mensaje de error")
    void productosRaizDesconocida() {
        PuntoRedProductosResponse r = parser.parseProductos("<ERROR><CODIGO>05</CODIGO><TEXTO>x</TEXTO></ERROR>");
        assertThat(r.getMensaje().getCodigo()).isEqualTo("05");
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @ValueSource(strings = {
            "<MENSAJE><TEXTO>sin código</TEXTO></MENSAJE>",
            "<PRODUCTOS></PRODUCTOS>",
            "<<no-es-xml",
            DECL + "<OTRO><TEXTO>x</TEXTO></OTRO>"})
    @DisplayName("Productos: documentos sin datos reconocibles se rechazan")
    void productosInvalidos(String xml) {
        assertThatThrownBy(() -> parser.parseProductos(xml)).isInstanceOf(PuntoRedRespuestaInvalidaException.class);
    }

    @Test
    @DisplayName("sendTx: MENSAJE con declaración XML y sin ID_TX")
    void sendTxMensajeConDeclaracion() {
        PuntoRedTxResponse r = parser.parseSendTx(DECL + "<MENSAJE><CODIGO>06</CODIGO><TEXTO>Duplicada</TEXTO></MENSAJE>");
        assertThat(r.getMensaje().getCodigo()).isEqualTo("06");
    }

    @Test
    @DisplayName("sendTx: acepta respuesta con solo ID_TX, solo autorización o solo MENSAJE")
    void sendTxCamposParciales() {
        assertThat(parser.parseSendTx(DECL + "<RESPUESTA><ID_TX>99</ID_TX><MENSAJE><CODIGO>01</CODIGO></MENSAJE></RESPUESTA>").getIdTx()).isEqualTo("99");
        assertThat(parser.parseSendTx("<RESPUESTA><ID_TX>1</ID_TX></RESPUESTA>").getIdTx()).isEqualTo("1");
        assertThat(parser.parseSendTx("<RESPUESTA><NUM_AUTORIZACION>A1</NUM_AUTORIZACION></RESPUESTA>").getNumAutorizacion()).isEqualTo("A1");
        assertThat(parser.parseSendTx("<RESPUESTA><MENSAJE><CODIGO>01</CODIGO></MENSAJE></RESPUESTA>").getMensaje().getCodigo()).isEqualTo("01");
        assertThat(parser.parseSendTx("<ERROR><CODIGO>10</CODIGO></ERROR>").getMensaje().getCodigo()).isEqualTo("10");
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @ValueSource(strings = {
            "<MENSAJE><TEXTO>sin código</TEXTO></MENSAJE>",
            "<RESPUESTA><SALDO>1</SALDO></RESPUESTA>",
            "<<no-es-xml"})
    @DisplayName("sendTx: documentos sin campos reconocibles se rechazan")
    void sendTxInvalidos(String xml) {
        assertThatThrownBy(() -> parser.parseSendTx(xml)).isInstanceOf(PuntoRedRespuestaInvalidaException.class);
    }

    @Test
    @DisplayName("MENSAJE genérico: vacío o ilegible se rechaza")
    void mensajeGenerico() {
        assertThat(parser.parseMensaje("<MENSAJE><CODIGO>01</CODIGO></MENSAJE>").isExitoso()).isTrue();
        assertThatThrownBy(() -> parser.parseMensaje(null)).isInstanceOf(PuntoRedRespuestaInvalidaException.class);
        assertThatThrownBy(() -> parser.parseMensaje(" ")).isInstanceOf(PuntoRedRespuestaInvalidaException.class);
        assertThatThrownBy(() -> parser.parseMensaje("<<x")).isInstanceOf(PuntoRedRespuestaInvalidaException.class);
    }
}
