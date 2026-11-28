package com.kapm.onboarding_clientes.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kapm.onboarding_clientes.entity.catalogo.CatalogoProductoPuntoRed;
import com.kapm.onboarding_clientes.repository.CatalogoProductoPuntoRedRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Carga inicial del catálogo de productos desde {@code catalogo/productos_gestopago.json} (exportación del
 * catálogo de Gestopago). Solo inserta los productos que aún no existen: nunca sobrescribe lo que ya está en
 * la tabla, ni lo que trajo la sincronización ni lo que se configuró a mano. Después completa los logos vacíos
 * con los logos públicos de Gestopago.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CatalogoSemillaLoader implements ApplicationRunner {

    private static final String RECURSO = "catalogo/productos_gestopago.json";
    /** Íconos de Google que asignaba una versión anterior; se sustituyen por el logo de Gestopago. */
    private static final String PREFIJO_ICONO_ANTERIOR = "https://www.google.com/s2/favicons";

    /** Mismos tipos que el catálogo considera vendibles (recargas y servicios). */
    private static final Set<Integer> TIPOS_FRONT_VENDIBLES = Set.of(0, 1, 2);

    /** Nombres de categoría por idCatTipoServicio de Gestopago. */
    private static final Map<Integer, String> CATEGORIAS = Map.ofEntries(
            Map.entry(1, "Ahorro y AFORE"),
            Map.entry(3, "Recargas telefónicas"),
            Map.entry(5, "Telefonía, internet y colegiaturas"),
            Map.entry(8, "Otros servicios"),
            Map.entry(9, "Venta por catálogo"),
            Map.entry(10, "Tarjetas de regalo"),
            Map.entry(11, "Entretenimiento"),
            Map.entry(13, "Paquetes de datos"),
            Map.entry(15, "Agua"),
            Map.entry(19, "Internet en casa"));

    private final CatalogoProductoPuntoRedRepository catalogoRepository;
    private final PuntoRedProperties puntoRedProperties;

    @Value("${app.catalogo.semilla.enabled:true}")
    private boolean habilitada;

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ProductoSemilla(String servicio, String nombreProducto, String idServicio, String idProducto,
                           String idCatTipoServicio, String tipoFront, String precio, String tipoReferencia,
                           String legend) {
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!habilitada) {
            return;
        }
        List<ProductoSemilla> semilla;
        // ObjectMapper propio: el global rechaza propiedades desconocidas
        ObjectMapper mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        try (InputStream in = new ClassPathResource(RECURSO).getInputStream()) {
            semilla = mapper.readValue(in, new TypeReference<>() { });
        } catch (Exception e) {
            log.warn("No se pudo leer la semilla del catálogo ({}): {}", RECURSO, e.getMessage());
            return;
        }

        int insertados = 0;
        int leyendasActualizadas = 0;
        for (ProductoSemilla p : semilla) {
            Integer idProducto = entero(p.idProducto());
            Integer idServicio = entero(p.idServicio());
            Integer tipoFront = entero(p.tipoFront());
            if (idProducto == null || idServicio == null || tipoFront == null) {
                continue;
            }
            if (catalogoRepository.existsByIdProducto(idProducto)) {
                String leyenda = descripcion(p.legend());
                if (leyenda != null) {
                    var existenteOpt = catalogoRepository.findByIdProducto(idProducto);
                    if (existenteOpt.isPresent()) {
                        CatalogoProductoPuntoRed existente = existenteOpt.get();
                        if (existente.getDescripcion() == null || existente.getDescripcion().isBlank()) {
                            existente.setDescripcion(leyenda);
                            catalogoRepository.save(existente);
                            leyendasActualizadas++;
                        }
                    }
                }
                continue;
            }
            catalogoRepository.save(aEntidad(p, idProducto, idServicio, tipoFront));
            insertados++;
        }
        if (insertados > 0) {
            log.info("Semilla del catálogo: {} productos insertados", insertados);
        }
        if (leyendasActualizadas > 0) {
            log.info("Semilla del catálogo: leyenda completada en {} productos existentes", leyendasActualizadas);
        }
        asignarLogos();
    }

    /**
     * Completa el logo de los productos que no tienen uno con el logo público de Gestopago (por idServicio).
     * También reemplaza los íconos de Google que guardaban versiones anteriores; un logo propio nunca se toca.
     */
    private void asignarLogos() {
        int asignados = 0;
        for (CatalogoProductoPuntoRed producto : catalogoRepository.findAll()) {
            String actual = producto.getLogoUrl();
            boolean reemplazable = actual == null || actual.isBlank() || actual.startsWith(PREFIJO_ICONO_ANTERIOR);
            if (reemplazable && producto.getIdServicio() != null) {
                producto.setLogoUrl(puntoRedProperties.logoServicio(producto.getIdServicio()));
                catalogoRepository.save(producto);
                asignados++;
            }
        }
        if (asignados > 0) {
            log.info("Semilla del catálogo: logo de Gestopago asignado a {} productos", asignados);
        }
    }

    private static CatalogoProductoPuntoRed aEntidad(ProductoSemilla p, Integer idProducto, Integer idServicio, Integer tipoFront) {
        Integer idCatTipo = entero(p.idCatTipoServicio());
        BigDecimal precio = decimal(p.precio());
        // tipoFront 1 = producto de precio fijo (recargas, paquetes, tarjetas, boletos); 2 = pago de recibo con monto libre
        BigDecimal montoFijo = tipoFront == 1 && precio != null && precio.signum() > 0 ? precio : null;
        String servicio = texto(p.servicio());

        return CatalogoProductoPuntoRed.builder()
                .idProducto(idProducto)
                .idServicio(idServicio)
                .nombreProducto(texto(p.nombreProducto()) != null ? texto(p.nombreProducto()) : servicio)
                .nombreServicio(servicio != null ? servicio : "Servicio " + idServicio)
                .idCatTipoServicio(idCatTipo)
                .tipoFront(tipoFront)
                .activo(TIPOS_FRONT_VENDIBLES.contains(tipoFront))
                .montoMinimo(montoFijo)
                .montoMaximo(montoFijo)
                .categoria(idCatTipo != null ? CATEGORIAS.getOrDefault(idCatTipo, "Otros servicios") : null)
                .descripcion(descripcion(p.legend()))
                .tipoReferencia(texto(p.tipoReferencia()))
                // Las rutas de la exportación son relativas a otro servidor; asignarLogos pone la de Gestopago
                .logoUrl(null)
                .build();
    }

    private static Integer entero(String valor) {
        try {
            return valor == null ? null : Integer.valueOf(valor.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static BigDecimal decimal(String valor) {
        try {
            return valor == null ? null : new BigDecimal(valor.trim()).setScale(2, java.math.RoundingMode.HALF_UP);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String texto(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim().replaceAll("\\s+", " ");
    }

    /** La leyenda trae algunas etiquetas HTML; se guarda como texto plano. */
    private static String descripcion(String legend) {
        String limpio = texto(legend == null ? null : legend.replaceAll("<[^>]*>", " "));
        return limpio == null || limpio.equals(".") ? null : limpio;
    }
}
