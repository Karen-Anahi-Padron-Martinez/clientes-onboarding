package com.kapm.onboarding_clientes.service;

import com.kapm.onboarding_clientes.client.puntored.PuntoRedClient;
import com.kapm.onboarding_clientes.client.puntored.model.PuntoRedProducto;
import com.kapm.onboarding_clientes.client.puntored.model.PuntoRedProductosResponse;
import com.kapm.onboarding_clientes.config.PuntoRedProperties;
import com.kapm.onboarding_clientes.entity.catalogo.CatalogoProductoPuntoRed;
import com.kapm.onboarding_clientes.exception.ErrorMessage;
import com.kapm.onboarding_clientes.exception.ReglaNegocioException;
import com.kapm.onboarding_clientes.repository.CatalogoProductoPuntoRedRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CatalogoPuntoRedSyncServiceImpl implements CatalogoPuntoRedSyncService {

    private final PuntoRedClient puntoRedClient;
    private final CatalogoProductoPuntoRedRepository catalogoRepository;
    private final CuotaConsultaProveedorService cuotaService;
    private final PuntoRedProperties properties;

    static final String OPERACION = "getProductList";

    @Override
    @Transactional
    public int sincronizar(boolean forzar) {
        int activosLocales = catalogoRepository.findByActivoTrue().size();

        // Con catálogo local vacío (primer despliegue) siempre se intenta, aunque la última sincronización sea reciente
        if (!forzar && activosLocales > 0 && sincronizacionReciente()) {
            log.info("Catálogo PuntoRed sincronizado hace menos de {} h; se omite la consulta para cuidar el cupo diario",
                    properties.getCatalogoHorasEntreSincronizaciones());
            return activosLocales;
        }

        // El cupo se consume en su propia transacción ANTES de la llamada: un intento fallido también cuenta
        if (!cuotaService.reservarConsulta(OPERACION, properties.getCatalogoLimiteDiario())) {
            if (forzar) {
                throw new ReglaNegocioException(ErrorMessage.LIMITE_CONSULTAS_CATALOGO);
            }
            return activosLocales;
        }

        // La llamada HTTP ocurre dentro de la transacción, pero solo toca la tabla de catálogo (sin locks de saldo)
        PuntoRedProductosResponse respuesta = puntoRedClient.getProductList();
        if (respuesta == null || respuesta.getProductos() == null || respuesta.getProductos().isEmpty()) {
            // Una respuesta vacía NO desactiva el catálogo: podría ser una falla transitoria del proveedor
            log.warn("PuntoRed devolvió un catálogo vacío; se conserva el catálogo local sin cambios");
            return activosLocales;
        }

        Map<Integer, PuntoRedProducto> remotos = respuesta.getProductos().stream()
                .filter(p -> p.getIdProducto() != null && p.getIdServicio() != null && p.getTipoFront() != null)
                .collect(Collectors.toMap(PuntoRedProducto::getIdProducto, Function.identity(), (a, b) -> a));

        Map<Integer, CatalogoProductoPuntoRed> locales = catalogoRepository.findAll().stream()
                .collect(Collectors.toMap(CatalogoProductoPuntoRed::getIdProducto, Function.identity()));

        remotos.forEach((idProducto, remoto) -> {
            CatalogoProductoPuntoRed local = locales.getOrDefault(idProducto,
                    CatalogoProductoPuntoRed.builder().idProducto(idProducto).build());
            local.setIdServicio(remoto.getIdServicio());
            local.setNombreProducto(Objects.requireNonNullElse(remoto.getProducto(), "SIN_NOMBRE"));
            local.setNombreServicio(Objects.requireNonNullElse(remoto.getServicio(), "SIN_NOMBRE"));
            local.setIdCatTipoServicio(remoto.getIdCatTipoServicio());
            local.setTipoFront(remoto.getTipoFront());
            local.setActivo(true);
            catalogoRepository.save(local);
        });

        locales.values().stream()
                .filter(local -> !remotos.containsKey(local.getIdProducto()) && Boolean.TRUE.equals(local.getActivo()))
                .forEach(local -> {
                    local.setActivo(false);
                    catalogoRepository.save(local);
                });

        cuotaService.registrarExito(OPERACION);
        log.info("Catálogo PuntoRed sincronizado: {} productos activos", remotos.size());
        return remotos.size();
    }

    private boolean sincronizacionReciente() {
        LocalDateTime limite = LocalDateTime.now().minusHours(properties.getCatalogoHorasEntreSincronizaciones());
        return cuotaService.ultimaConsultaExitosa(OPERACION).map(ultima -> ultima.isAfter(limite)).orElse(false);
    }
}
