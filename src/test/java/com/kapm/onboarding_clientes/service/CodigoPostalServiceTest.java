package com.kapm.onboarding_clientes.service;

import com.kapm.onboarding_clientes.dto.response.CodigoPostalResponse;
import com.kapm.onboarding_clientes.exception.RecursoNoEncontradoException;
import com.kapm.onboarding_clientes.exception.ReglaNegocioException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("CodigoPostalService: autocompletado con el catálogo de Correos de México")
class CodigoPostalServiceTest {

    /** Mismo formato que cpdescarga.txt: aviso, encabezados y registros separados por |. */
    private static final String MUESTRA = String.join("\n",
            "El Catálogo Nacional de Códigos Postales, es elaborado por Correos de México...",
            "d_codigo|d_asenta|d_tipo_asenta|D_mnpio|d_estado|d_ciudad|d_CP|c_estado|c_oficina|c_CP|c_tipo_asenta|c_mnpio|id_asenta_cpcons|d_zona|c_cve_ciudad",
            "06600|Juárez|Colonia|Cuauhtémoc|Ciudad de México|Ciudad de México|06721|09|06721||09|015|0930|Urbano|06",
            "20010|Ramon Romo Franco|Colonia|Aguascalientes|Aguascalientes|Aguascalientes|20001|01|20001||09|001|0001|Urbano|01",
            "20010|Colinas del Rio|Colonia|Aguascalientes|Aguascalientes|Aguascalientes|20001|01|20001||09|001|0002|Urbano|01",
            "20900|Centro|Colonia|Jesús María|Aguascalientes|Jesús María|20900|01|20900||09|005|0001|Urbano|01",
            "25000|Zona Centro|Colonia|Saltillo|Coahuila de Zaragoza|Saltillo|25001|05|25001||09|030|0001|Urbano|01",
            "37800|Centro Dolores Hidalgo|Colonia|Dolores Hidalgo Cuna de la Independencia Nacional|Guanajuato|Dolores Hidalgo Cuna de la Independencia Nacional|37801|11|37801||09|014|1951|Urbano|39",
            "37803|Lázaro Cárdenas|Colonia|Dolores Hidalgo Cuna de la Independencia Nacional|Guanajuato|Dolores Hidalgo Cuna de la Independencia Nacional|37801|11|37801||09|014|0026|Urbano|39",
            "37804|Compositores|Colonia|Dolores Hidalgo Cuna de la Independencia Nacional|Guanajuato|Dolores Hidalgo Cuna de la Independencia Nacional|37801|11|37801||09|014|0019|Urbano|39");

    @TempDir
    Path dir;

    private CodigoPostalService servicio() throws Exception {
        Path cache = dir.resolve("cpdescarga.txt");
        // El archivo oficial viene en ISO-8859-1
        Files.writeString(cache, MUESTRA, StandardCharsets.ISO_8859_1);
        return new CodigoPostalService("http://localhost:1/no-se-usa", cache.toString(), 30, false);
    }

    @Test
    @DisplayName("Devuelve estado y país como códigos de catálogo, municipio y colonias con acentos")
    void consulta() throws Exception {
        CodigoPostalResponse r = servicio().consultar("06600");
        assertThat(r.getEstado()).isEqualTo("CIUDAD_DE_MEXICO");
        assertThat(r.getPais()).isEqualTo("MEXICO");
        assertThat(r.getMunicipio()).isEqualTo("Cuauhtémoc");
        assertThat(r.getColonias()).containsExactly("Juárez");
    }

    @Test
    @DisplayName("Agrupa las colonias del mismo código y usa la clave INEGI del estado")
    void agrupaColonias() throws Exception {
        CodigoPostalService s = servicio();
        assertThat(s.consultar("20010").getColonias()).containsExactly("Ramon Romo Franco", "Colinas del Rio");
        assertThat(s.consultar("25000").getEstado()).isEqualTo("COAHUILA");
    }

    @Test
    @DisplayName("Devuelve los municipios ordenados y sin duplicados de un estado")
    void municipiosPorEstado() throws Exception {
        CodigoPostalService s = servicio();
        assertThat(s.municipiosPorEstado("AGUASCALIENTES")).containsExactly("Aguascalientes", "Jesús María");
        assertThat(s.municipiosPorEstado("ciudad_de_mexico")).containsExactly("Cuauhtémoc");
        assertThat(s.municipiosPorEstado("INEXISTENTE")).isEmpty();
        assertThat(s.municipiosPorEstado("")).isEmpty();
    }

    @Test
    @DisplayName("Al consultar 37800 incluye Centro Dolores Hidalgo primero y el resto de colonias de Dolores Hidalgo")
    void coloniasDeDoloresHidalgoConCp37800() throws Exception {
        CodigoPostalService s = servicio();
        CodigoPostalResponse r = s.consultar("37800");
        assertThat(r.getEstado()).isEqualTo("GUANAJUATO");
        assertThat(r.getMunicipio()).isEqualTo("Dolores Hidalgo Cuna de la Independencia Nacional");
        assertThat(r.getColonias()).containsExactly("Centro Dolores Hidalgo", "Compositores", "Lázaro Cárdenas");

        assertThat(s.coloniasPorMunicipio("GUANAJUATO", "Dolores Hidalgo Cuna de la Independencia Nacional"))
                .containsExactly("Centro Dolores Hidalgo", "Compositores", "Lázaro Cárdenas");
        assertThat(s.coloniasPorMunicipio("", "Dolores Hidalgo")).isEmpty();
    }

    @Test
    @DisplayName("Código inexistente responde 404 y formato inválido 400")
    void errores() throws Exception {
        CodigoPostalService s = servicio();
        assertThatThrownBy(() -> s.consultar("99999")).isInstanceOf(RecursoNoEncontradoException.class);
        assertThatThrownBy(() -> s.consultar("12a")).isInstanceOf(ReglaNegocioException.class);
    }
}
