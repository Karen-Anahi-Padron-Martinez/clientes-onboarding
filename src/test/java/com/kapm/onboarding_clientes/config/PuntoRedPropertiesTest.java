package com.kapm.onboarding_clientes.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Pruebas Unitarias - PuntoRedProperties")
class PuntoRedPropertiesTest {

    @Test
    @DisplayName("Valores por defecto correctos para zonaHoraria y authPath")
    void testValoresPorDefecto() {
        PuntoRedProperties properties = new PuntoRedProperties();

        assertThat(properties.getZonaHoraria()).isEqualTo("America/Mexico_City");
        assertThat(properties.getAuthPath()).isEqualTo("/sistema/app/jwt-gp/authenticate/");
        assertThat(properties.getConnectTimeout()).isEqualTo(5000);
        assertThat(properties.getReadTimeout()).isEqualTo(70000);
    }

    @Test
    @DisplayName("Asignación y obtención de propiedades de autenticación y terminal")
    void testPropiedadesAutenticacionYTerminal() {
        PuntoRedProperties properties = new PuntoRedProperties();
        properties.setIdDistribuidor("12345");
        properties.setCodigoDispositivo("DISP-01");
        properties.setPassword("SecretPass");
        properties.setUnidad("SUCURSAL_NORTE");
        properties.setZonaHoraria("America/Cancun");
        properties.setAuthPath("/custom/auth/");

        assertThat(properties.getIdDistribuidor()).isEqualTo("12345");
        assertThat(properties.getIdDistribuidorAsInteger()).isEqualTo(12345);
        assertThat(properties.getCodigoDispositivo()).isEqualTo("DISP-01");
        assertThat(properties.getPassword()).isEqualTo("SecretPass");
        assertThat(properties.getUnidad()).isEqualTo("SUCURSAL_NORTE");
        assertThat(properties.getZonaHoraria()).isEqualTo("America/Cancun");
        assertThat(properties.getAuthPath()).isEqualTo("/custom/auth/");
    }

    @Test
    @DisplayName("Conversión segura de idDistribuidor a Integer cuando es nulo o no numérico")
    void testIdDistribuidorAsIntegerSeguro() {
        PuntoRedProperties properties = new PuntoRedProperties();

        properties.setIdDistribuidor(null);
        assertThat(properties.getIdDistribuidorAsInteger()).isNull();

        properties.setIdDistribuidor("   ");
        assertThat(properties.getIdDistribuidorAsInteger()).isNull();

        properties.setIdDistribuidor("NO_NUMERICO");
        assertThat(properties.getIdDistribuidorAsInteger()).isNull();
    }
}
