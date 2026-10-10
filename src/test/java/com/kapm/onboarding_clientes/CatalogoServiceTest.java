package com.kapm.onboarding_clientes;

import com.kapm.onboarding_clientes.dto.response.CatalogosResponse;
import com.kapm.onboarding_clientes.exception.ReglaNegocioException;
import com.kapm.onboarding_clientes.service.CatalogoService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class CatalogoServiceTest {

    @Autowired
    private CatalogoService catalogoService;

    @Test
    @DisplayName("Debe obtener todos los catálogos con sus colecciones pobladas")
    void debeObtenerTodosLosCatalogos() {
        CatalogosResponse catalogos = catalogoService.obtenerTodosLosCatalogos();

        assertNotNull(catalogos);
        assertFalse(catalogos.getSexos().isEmpty());
        assertFalse(catalogos.getEstadosCiviles().isEmpty());
        assertFalse(catalogos.getNacionalidades().isEmpty());
        assertFalse(catalogos.getOcupaciones().isEmpty());
        assertEquals(32, catalogos.getEstadosRepublica().size(), "Deben ser exactamente 32 entidades federativas");
        assertFalse(catalogos.getPaises().isEmpty());
        assertEquals(4, catalogos.getTiposBiometria().size(), "Deben ser 4 modalidades biométricas");
        assertEquals(3, catalogos.getEstatusCuenta().size(), "Deben ser 3 estatus de cuenta");
    }

    @Test
    @DisplayName("Debe normalizar correctamente los valores válidos de catálogo")
    void debeNormalizarValoresValidos() {
        assertEquals("MASCULINO", catalogoService.normalizarSexo("masculino"));
        assertEquals("FEMENINO", catalogoService.normalizarSexo("FEMENINO"));
        assertEquals("SOLTERO", catalogoService.normalizarEstadoCivil("soltero"));
        assertEquals("UNION_LIBRE", catalogoService.normalizarEstadoCivil("UNION_LIBRE"));
        assertEquals("Ciudad de México", catalogoService.normalizarEstadoRepublica("CIUDAD_DE_MEXICO"));
        assertEquals("Jalisco", catalogoService.normalizarEstadoRepublica("jalisco"));
        assertEquals("México", catalogoService.normalizarPais("MEXICO"));
        assertEquals("Mexicana", catalogoService.normalizarNacionalidad("mexicana"));
    }

    @Test
    @DisplayName("Debe rechazar sexo o estado civil fuera de catálogo")
    void debeRechazarValoresInvalidos() {
        assertThrows(ReglaNegocioException.class, () -> catalogoService.normalizarSexo("DESCONOCIDO"));
        assertThrows(ReglaNegocioException.class, () -> catalogoService.normalizarEstadoCivil("INEXISTENTE"));
    }
}
