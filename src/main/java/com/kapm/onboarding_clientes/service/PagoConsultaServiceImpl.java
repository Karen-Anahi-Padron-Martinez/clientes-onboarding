package com.kapm.onboarding_clientes.service;

import com.kapm.onboarding_clientes.config.PuntoRedProperties;
import com.kapm.onboarding_clientes.dto.request.HistorialPagosRequest;
import com.kapm.onboarding_clientes.dto.request.ValidarReferenciaRequest;
import com.kapm.onboarding_clientes.dto.response.PageResponse;
import com.kapm.onboarding_clientes.dto.response.PagoResponse;
import com.kapm.onboarding_clientes.dto.response.ProductoPagoResponse;
import com.kapm.onboarding_clientes.dto.response.ValidacionReferenciaResponse;
import com.kapm.onboarding_clientes.entity.PagoServicio;
import com.kapm.onboarding_clientes.entity.catalogo.CatalogoProductoPuntoRed;
import com.kapm.onboarding_clientes.exception.ErrorMessage;
import com.kapm.onboarding_clientes.exception.RecursoNoEncontradoException;
import com.kapm.onboarding_clientes.exception.ReglaNegocioException;
import com.kapm.onboarding_clientes.repository.CatalogoProductoPuntoRedRepository;
import com.kapm.onboarding_clientes.repository.PagoServicioRepository;
import com.kapm.onboarding_clientes.util.ReferenciaUtil;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PagoConsultaServiceImpl implements PagoConsultaService {

    private static final Set<Integer> TIPOS_FRONT_PERMITIDOS = Set.of(0, 1, 2);
    private static final int TAMANIO_DEFAULT = 10;

    private final CatalogoProductoPuntoRedRepository catalogoRepository;
    private final PagoServicioRepository pagoServicioRepository;
    private final ClienteService clienteService;
    private final PuntoRedProperties puntoRedProperties;

    @Override
    public List<ProductoPagoResponse> listarCatalogo() {
        return catalogoRepository.findByActivoTrue().stream()
                .filter(this::esVendible)
                .map(this::aProducto)
                .sorted(Comparator.comparing(ProductoPagoResponse::getCategoria)
                        .thenComparing(ProductoPagoResponse::getNombreProducto))
                .toList();
    }

    @Override
    public ValidacionReferenciaResponse validarReferencia(ValidarReferenciaRequest request) {
        CatalogoProductoPuntoRed producto = catalogoRepository.findByIdProductoAndActivoTrue(request.getIdProducto())
                .filter(p -> p.getIdServicio().equals(request.getIdServicio()))
                .filter(this::esVendible)
                .orElseThrow(() -> new ReglaNegocioException(ErrorMessage.PRODUCTO_NO_PERMITIDO));

        ProductoPagoResponse datosProducto = aProducto(producto);

        // Mismas reglas que PagoServicioServiceImpl.validarOperacion aplica antes de cobrar
        if (ReferenciaUtil.esRecarga(producto)) {
            String telefono = request.getTelefono() != null ? request.getTelefono().trim() : "";
            if (!telefono.matches("^\\d{10}$")) {
                return invalida(ErrorMessage.TELEFONO_REQUERIDO_RECARGA, datosProducto);
            }
            if (!ReferenciaUtil.cumpleRegex(producto, telefono)) {
                return invalida(ErrorMessage.REFERENCIA_FORMATO_INVALIDO, datosProducto);
            }
            return ValidacionReferenciaResponse.builder()
                    .valida(true).mensaje("Número de teléfono válido").telefono(telefono).producto(datosProducto).build();
        }

        String referencia = request.getReferencia() != null ? request.getReferencia().trim() : "";
        if (referencia.isEmpty()) {
            return invalida(ErrorMessage.REFERENCIA_REQUERIDA_SERVICIO, datosProducto);
        }
        if (!ReferenciaUtil.cumpleRegex(producto, referencia)) {
            return invalida(ErrorMessage.REFERENCIA_FORMATO_INVALIDO, datosProducto);
        }
        return ValidacionReferenciaResponse.builder()
                .valida(true).mensaje("Referencia válida").referencia(referencia).producto(datosProducto).build();
    }

    @Override
    public PageResponse<PagoResponse> historial(HistorialPagosRequest request, String username) {
        String curpTitular = clienteService.obtenerCurpPorUsername(username);

        if (request.getFechaDesde() != null && request.getFechaHasta() != null
                && request.getFechaDesde().isAfter(request.getFechaHasta())) {
            throw new ReglaNegocioException(ErrorMessage.RANGO_FECHAS_INVALIDO);
        }

        Specification<PagoServicio> filtro = (root, query, cb) -> {
            List<Predicate> predicados = new ArrayList<>();
            predicados.add(cb.equal(root.get("curp"), curpTitular));
            if (request.getEstatus() != null) {
                predicados.add(cb.equal(root.get("estatus"), request.getEstatus()));
            }
            if (request.getFechaDesde() != null) {
                predicados.add(cb.greaterThanOrEqualTo(root.get("fechaCreacion"), request.getFechaDesde().atStartOfDay()));
            }
            if (request.getFechaHasta() != null) {
                predicados.add(cb.lessThan(root.get("fechaCreacion"), request.getFechaHasta().plusDays(1).atStartOfDay()));
            }
            return cb.and(predicados.toArray(Predicate[]::new));
        };

        int pagina = request.getPagina() != null ? request.getPagina() : 0;
        int tamanio = request.getTamanio() != null ? request.getTamanio() : TAMANIO_DEFAULT;
        Page<PagoServicio> pagos = pagoServicioRepository.findAll(filtro,
                PageRequest.of(pagina, tamanio, Sort.by(Sort.Direction.DESC, "fechaCreacion")));

        Map<Integer, CatalogoProductoPuntoRed> productos = catalogoRepository
                .findByIdProductoIn(pagos.getContent().stream().map(PagoServicio::getIdProducto).collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(CatalogoProductoPuntoRed::getIdProducto, Function.identity()));

        List<PagoResponse> contenido = pagos.getContent().stream()
                .map(p -> PagoResponse.desde(p, null, productos.get(p.getIdProducto())))
                .toList();
        return PageResponse.from(new PageImpl<>(contenido, pagos.getPageable(), pagos.getTotalElements()));
    }

    @Override
    public PagoResponse detalle(String upc, String username) {
        String curpTitular = clienteService.obtenerCurpPorUsername(username);
        PagoServicio pago = pagoServicioRepository.findByUpc(upc.trim())
                .filter(p -> curpTitular.equals(p.getCurp()))
                .orElseThrow(() -> new RecursoNoEncontradoException(ErrorMessage.PAGO_NO_ENCONTRADO));
        CatalogoProductoPuntoRed producto = catalogoRepository.findByIdProducto(pago.getIdProducto()).orElse(null);
        return PagoResponse.desde(pago, null, producto);
    }

    /** Solo recargas (0) y servicios (1, 2): lo mismo que acepta el flujo de pago. */
    private boolean esVendible(CatalogoProductoPuntoRed p) {
        return p.getTipoFront() != null && TIPOS_FRONT_PERMITIDOS.contains(p.getTipoFront());
    }

    private ValidacionReferenciaResponse invalida(ErrorMessage motivo, ProductoPagoResponse producto) {
        return ValidacionReferenciaResponse.builder().valida(false).mensaje(motivo.getMensaje()).producto(producto).build();
    }

    private ProductoPagoResponse aProducto(CatalogoProductoPuntoRed p) {
        boolean recarga = ReferenciaUtil.esRecarga(p);
        BigDecimal minimo = p.getMontoMinimo() != null ? p.getMontoMinimo() : puntoRedProperties.getMontoMinimoOperacion();
        BigDecimal maximo = p.getMontoMaximo() != null ? p.getMontoMaximo() : puntoRedProperties.getMontoMaximoOperacion();
        String categoria = p.getCategoria() != null && !p.getCategoria().isBlank()
                ? p.getCategoria().trim()
                : (recarga ? "Recargas" : "Servicios");

        return ProductoPagoResponse.builder()
                .idProducto(p.getIdProducto())
                .idServicio(p.getIdServicio())
                .nombreProducto(p.getNombreProducto())
                .nombreServicio(p.getNombreServicio())
                .idCatTipoServicio(p.getIdCatTipoServicio())
                .tipoFront(p.getTipoFront())
                .tipoOperacion(recarga ? "RECARGA" : "SERVICIO")
                .categoria(categoria)
                .descripcion(p.getDescripcion())
                // Logo configurado en BD o, si no hay, el público de Gestopago por idServicio
                .logoUrl(p.getLogoUrl() != null && !p.getLogoUrl().isBlank() ? p.getLogoUrl() : puntoRedProperties.logoServicio(p.getIdServicio()))
                .imagenAyudaUrl(recarga ? null : puntoRedProperties.imagenAyudaReferencia(p.getIdServicio()))
                .regexReferencia(p.getRegexReferencia())
                .montoMinimo(minimo)
                .montoMaximo(maximo)
                .montoFijo(minimo != null && maximo != null && minimo.compareTo(maximo) == 0)
                .build();
    }
}
