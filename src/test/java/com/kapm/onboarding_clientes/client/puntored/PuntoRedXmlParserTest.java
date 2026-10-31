package com.kapm.onboarding_clientes.client.puntored;

import com.kapm.onboarding_clientes.client.puntored.model.PuntoRedProductosResponse;
import com.kapm.onboarding_clientes.client.puntored.model.PuntoRedTxResponse;
import com.kapm.onboarding_clientes.exception.PuntoRedRespuestaInvalidaException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Pruebas Unitarias - PuntoRedXmlParser")
class PuntoRedXmlParserTest {

    private PuntoRedXmlParser parser;

    @BeforeEach
    void setUp() {
        parser = new PuntoRedXmlParser();
    }

    @Test
    @DisplayName("Parseo exitoso de getProductList con múltiples productos y atributos")
    void testParseProductosOk() {
        String xml = """
                <PRODUCTOS>
                    <MENSAJE>
                        <CODIGO>01</CODIGO>
                        <TEXTO>CONSULTA EXITOSA</TEXTO>
                    </MENSAJE>
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

        PuntoRedProductosResponse response = parser.parseProductos(xml);

        assertThat(response).isNotNull();
        assertThat(response.getMensaje()).isNotNull();
        assertThat(response.getMensaje().getCodigo()).isEqualTo("01");
        assertThat(response.getProductos()).hasSize(1);
        assertThat(response.getProductos().get(0).getProducto()).isEqualTo("CFE");
        assertThat(response.getProductos().get(0).getIdProducto()).isEqualTo(5001);
    }

    @Test
    @DisplayName("Parseo de getProductList cuando el proveedor solo responde un nodo raíz MENSAJE de error")
    void testParseProductosMensajeError() {
        String xml = """
                <MENSAJE>
                    <CODIGO>99</CODIGO>
                    <TEXTO>PROVEEDOR FUERA DE LINEA</TEXTO>
                </MENSAJE>
                """;

        PuntoRedProductosResponse response = parser.parseProductos(xml);

        assertThat(response).isNotNull();
        assertThat(response.getMensaje()).isNotNull();
        assertThat(response.getMensaje().getCodigo()).isEqualTo("99");
        assertThat(response.getProductos()).isEmpty();
    }

    @Test
    @DisplayName("Falla al parsear getProductList con XML vacío o ilegible")
    void testParseProductosXmlInvalido() {
        assertThatThrownBy(() -> parser.parseProductos(null))
                .isInstanceOf(PuntoRedRespuestaInvalidaException.class)
                .hasMessageContaining("vacía o nula");

        assertThatThrownBy(() -> parser.parseProductos("   "))
                .isInstanceOf(PuntoRedRespuestaInvalidaException.class)
                .hasMessageContaining("vacía o nula");

        assertThatThrownBy(() -> parser.parseProductos("NO_ES_XML_VALIDO"))
                .isInstanceOf(PuntoRedRespuestaInvalidaException.class)
                .hasMessageContaining("inesperada o no tiene un formato válido");
    }

    @Test
    @DisplayName("Parseo exitoso de sendTx con importes BigDecimal y FECHA en inglés ('25/Feb/2022 18:15:25')")
    void testParseSendTxOk() {
        String xml = """
                <RESPUESTA>
                    <MENSAJE>
                        <CODIGO>01</CODIGO>
                        <TEXTO>TRANSACCION EXITOSA</TEXTO>
                    </MENSAJE>
                    <ID_TX>TX-100200</ID_TX>
                    <NUM_AUTORIZACION>AUTH-5544</NUM_AUTORIZACION>
                    <SALDO>12500.50</SALDO>
                    <COMISION>15.00</COMISION>
                    <SALDO_F>12,500.50</SALDO_F>
                    <COMISION_F>15.00</COMISION_F>
                    <FECHA>25/Feb/2022 18:15:25</FECHA>
                    <MONTO>450.00</MONTO>
                </RESPUESTA>
                """;

        PuntoRedTxResponse response = parser.parseSendTx(xml);

        assertThat(response).isNotNull();
        assertThat(response.getIdTx()).isEqualTo("TX-100200");
        assertThat(response.getNumAutorizacion()).isEqualTo("AUTH-5544");
        assertThat(response.getSaldo()).isEqualByComparingTo(new BigDecimal("12500.50"));
        assertThat(response.getComision()).isEqualByComparingTo(new BigDecimal("15.00"));
        assertThat(response.getMonto()).isEqualByComparingTo(new BigDecimal("450.00"));
        assertThat(response.getFecha()).isEqualTo("25/Feb/2022 18:15:25");

        LocalDateTime fechaParsed = response.getFechaParsed();
        assertThat(fechaParsed).isNotNull();
        assertThat(fechaParsed.getYear()).isEqualTo(2022);
        assertThat(fechaParsed.getMonthValue()).isEqualTo(2); // Febrero
        assertThat(fechaParsed.getDayOfMonth()).isEqualTo(25);
        assertThat(fechaParsed.getHour()).isEqualTo(18);
        assertThat(fechaParsed.getMinute()).isEqualTo(15);
        assertThat(fechaParsed.getSecond()).isEqualTo(25);
    }

    @Test
    @DisplayName("Comportamiento con meses en español bajo Locale.ENGLISH: 'Dic' rompe con DateTimeParseException")
    void testFechaMesEspanolRompeConLocaleEnglish() {
        String xmlConMesEspanol = """
                <RESPUESTA>
                    <MENSAJE><CODIGO>01</CODIGO><TEXTO>OK</TEXTO></MENSAJE>
                    <ID_TX>TX-DIC-01</ID_TX>
                    <FECHA>25/Dic/2022 18:15:25</FECHA>
                </RESPUESTA>
                """;

        PuntoRedTxResponse response = parser.parseSendTx(xmlConMesEspanol);
        assertThat(response.getFecha()).isEqualTo("25/Dic/2022 18:15:25");

        // 'Dic' no existe en Locale.ENGLISH (en inglés es 'Dec'), por lo que arroja DateTimeParseException
        assertThatThrownBy(response::getFechaParsed)
                .isInstanceOf(DateTimeParseException.class)
                .hasMessageContaining("could not be parsed");
    }

    @Test
    @DisplayName("Falla al parsear sendTx con XML vacío o no reconocido")
    void testParseSendTxInvalido() {
        assertThatThrownBy(() -> parser.parseSendTx(""))
                .isInstanceOf(PuntoRedRespuestaInvalidaException.class)
                .hasMessageContaining("vacía o nula");

        assertThatThrownBy(() -> parser.parseSendTx("<DESCONOCIDO>sin campos</DESCONOCIDO>"))
                .isInstanceOf(PuntoRedRespuestaInvalidaException.class)
                .hasMessageContaining("inválida, inesperada");
    }
}
