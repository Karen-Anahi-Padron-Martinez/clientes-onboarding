package com.kapm.onboarding_clientes.config;

import com.kapm.onboarding_clientes.entity.catalogo.CatalogoProductoPuntoRed;
import com.kapm.onboarding_clientes.repository.CatalogoProductoPuntoRedRepository;
import com.kapm.onboarding_clientes.util.ReferenciaUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("CatalogoSemillaLoader: carga del catálogo desde productos_gestopago.json")
class CatalogoSemillaLoaderTest {

    private final CatalogoProductoPuntoRedRepository repository = mock(CatalogoProductoPuntoRedRepository.class);
    private final CatalogoSemillaLoader loader = new CatalogoSemillaLoader(repository, new PuntoRedProperties());

    private List<CatalogoProductoPuntoRed> cargar() {
        ReflectionTestUtils.setField(loader, "habilitada", true);
        loader.run(null);
        ArgumentCaptor<CatalogoProductoPuntoRed> guardados = ArgumentCaptor.forClass(CatalogoProductoPuntoRed.class);
        verify(repository, atLeastOnce()).save(guardados.capture());
        return guardados.getAllValues();
    }

    private static CatalogoProductoPuntoRed porId(List<CatalogoProductoPuntoRed> productos, int idProducto) {
        return productos.stream().filter(p -> p.getIdProducto() == idProducto).findFirst().orElseThrow();
    }

    @Test
    @DisplayName("Inserta los 101 productos con categoría, monto fijo y tipo de referencia")
    void insertaProductos() {
        List<CatalogoProductoPuntoRed> productos = cargar();
        assertThat(productos).hasSize(101);

        CatalogoProductoPuntoRed recargaAtt = porId(productos, 299);
        assertThat(recargaAtt.getNombreServicio()).isEqualTo("AT-T");
        assertThat(recargaAtt.getCategoria()).isEqualTo("Recargas telefónicas");
        assertThat(recargaAtt.getMontoMinimo()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(recargaAtt.getMontoMaximo()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(ReferenciaUtil.esRecarga(recargaAtt)).isTrue();
        assertThat(recargaAtt.getActivo()).isTrue();

        CatalogoProductoPuntoRed agua = porId(productos, 185);
        assertThat(agua.getCategoria()).isEqualTo("Agua");
        assertThat(agua.getMontoMinimo()).isNull();
        assertThat(ReferenciaUtil.esRecarga(agua)).isFalse();

        // Consulta de saldo (tipoFront 4) y envío de dinero (tipoFront 40) quedan inactivos
        assertThat(porId(productos, 398).getActivo()).isFalse();
        assertThat(porId(productos, 20107).getActivo()).isFalse();

        // La leyenda se guarda sin etiquetas HTML y sin logo relativo
        assertThat(porId(productos, 6023).getDescripcion()).doesNotContain("<br>");
        assertThat(productos).allMatch(p -> p.getLogoUrl() == null);
    }

    @Test
    @DisplayName("No sobrescribe productos que ya existen en la tabla")
    void respetaExistentes() {
        when(repository.existsByIdProducto(anyInt())).thenReturn(false);
        when(repository.existsByIdProducto(299)).thenReturn(true);
        List<CatalogoProductoPuntoRed> productos = cargar();
        assertThat(productos).hasSize(100).noneMatch(p -> p.getIdProducto() == 299);
    }

    @Test
    @DisplayName("Completa logos vacíos (y los íconos de Google anteriores) con el de Gestopago; respeta los propios")
    void asignaLogos() {
        CatalogoProductoPuntoRed att = CatalogoProductoPuntoRed.builder().idProducto(299).idServicio(115).build();
        CatalogoProductoPuntoRed propio = CatalogoProductoPuntoRed.builder().idProducto(298).idServicio(115)
                .logoUrl("https://cdn.ejemplo.com/att.png").build();
        CatalogoProductoPuntoRed anterior = CatalogoProductoPuntoRed.builder().idProducto(14302).idServicio(2284)
                .logoUrl("https://www.google.com/s2/favicons?sz=128&domain=abib.mx").build();
        when(repository.existsByIdProducto(anyInt())).thenReturn(true);
        when(repository.findAll()).thenReturn(List.of(att, propio, anterior));

        ReflectionTestUtils.setField(loader, "habilitada", true);
        loader.run(null);

        assertThat(att.getLogoUrl()).isEqualTo("https://gestopago.portalventas.net/sistema/images/gestopago/servicios/115.png");
        assertThat(anterior.getLogoUrl()).isEqualTo("https://gestopago.portalventas.net/sistema/images/gestopago/servicios/2284.png");
        assertThat(propio.getLogoUrl()).isEqualTo("https://cdn.ejemplo.com/att.png");
        verify(repository, never()).save(propio);
    }

    @Test
    @DisplayName("Deshabilitada no toca la base de datos")
    void deshabilitada() {
        ReflectionTestUtils.setField(loader, "habilitada", false);
        loader.run(null);
        verify(repository, never()).existsByIdProducto(eq(299));
    }
}
