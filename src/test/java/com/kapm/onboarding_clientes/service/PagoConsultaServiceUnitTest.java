package com.kapm.onboarding_clientes.service;

import com.kapm.onboarding_clientes.config.PuntoRedProperties;
import com.kapm.onboarding_clientes.dto.request.HistorialPagosRequest;
import com.kapm.onboarding_clientes.dto.request.ValidarReferenciaRequest;
import com.kapm.onboarding_clientes.dto.response.PageResponse;
import com.kapm.onboarding_clientes.dto.response.PagoResponse;
import com.kapm.onboarding_clientes.dto.response.ProductoPagoResponse;
import com.kapm.onboarding_clientes.dto.response.ValidacionReferenciaResponse;
import com.kapm.onboarding_clientes.entity.EstatusPago;
import com.kapm.onboarding_clientes.entity.PagoServicio;
import com.kapm.onboarding_clientes.entity.catalogo.CatalogoProductoPuntoRed;
import com.kapm.onboarding_clientes.exception.RecursoNoEncontradoException;
import com.kapm.onboarding_clientes.exception.ReglaNegocioException;
import com.kapm.onboarding_clientes.repository.CatalogoProductoPuntoRedRepository;
import com.kapm.onboarding_clientes.repository.PagoServicioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("PagoConsultaServiceImpl (unitarias): catálogo, validación de referencia, historial y detalle")
class PagoConsultaServiceUnitTest {

    private static final String USUARIO = "titular";
    private static final String CURP = "MEHC920824HDFRMN09";

    @Mock
    private CatalogoProductoPuntoRedRepository catalogoRepository;
    @Mock
    private PagoServicioRepository pagoServicioRepository;
    @Mock
    private ClienteService clienteService;

    private PagoConsultaServiceImpl service;

    @BeforeEach
    void setUp() {
        PuntoRedProperties props = new PuntoRedProperties();
        props.setMontoMinimoOperacion(new BigDecimal("1.00"));
        props.setMontoMaximoOperacion(new BigDecimal("5000.00"));
        service = new PagoConsultaServiceImpl(catalogoRepository, pagoServicioRepository, clienteService, props);
        when(clienteService.obtenerCurpPorUsername(USUARIO)).thenReturn(CURP);
    }

    private CatalogoProductoPuntoRed producto(int idProducto, int tipoFront, String regex) {
        return CatalogoProductoPuntoRed.builder()
                .idProducto(idProducto).idServicio(100 + idProducto)
                .nombreProducto("Producto " + idProducto).nombreServicio("Servicio " + idProducto)
                .tipoFront(tipoFront).activo(true).regexReferencia(regex)
                .build();
    }

    private ValidarReferenciaRequest solicitud(CatalogoProductoPuntoRed p, String referencia, String telefono) {
        return ValidarReferenciaRequest.builder()
                .idProducto(p.getIdProducto()).idServicio(p.getIdServicio())
                .referencia(referencia).telefono(telefono).build();
    }

    @Test
    @DisplayName("El catálogo omite tipos no vendibles, aplica límites globales y categoría por defecto")
    void catalogo() {
        CatalogoProductoPuntoRed recarga = producto(1, 0, null);
        CatalogoProductoPuntoRed servicio = producto(2, 1, null);
        servicio.setCategoria("Luz");
        servicio.setMontoMinimo(new BigDecimal("50.00"));
        servicio.setMontoMaximo(new BigDecimal("50.00"));
        when(catalogoRepository.findByActivoTrue()).thenReturn(List.of(recarga, servicio, producto(3, 7, null)));

        List<ProductoPagoResponse> lista = service.listarCatalogo();

        assertThat(lista).extracting(ProductoPagoResponse::getIdProducto).containsExactly(2, 1);
        ProductoPagoResponse r = lista.get(1);
        assertThat(r.getCategoria()).isEqualTo("Recargas");
        assertThat(r.getTipoOperacion()).isEqualTo("RECARGA");
        assertThat(r.getMontoMaximo()).isEqualByComparingTo("5000.00");
        assertThat(lista.get(0).isMontoFijo()).isTrue();

        // Sin logo en BD se usa el público de Gestopago; la ayuda de referencia solo aplica a servicios
        String base = "https://gestopago.portalventas.net/sistema/images/gestopago";
        assertThat(r.getLogoUrl()).isEqualTo(base + "/servicios/101.png");
        assertThat(r.getImagenAyudaUrl()).isNull();
        assertThat(lista.get(0).getImagenAyudaUrl()).isEqualTo(base + "/referencias/102.jpg");
    }

    @Test
    @DisplayName("Referencia que cumple la expresión del producto es válida")
    void referenciaValida() {
        CatalogoProductoPuntoRed p = producto(2, 1, "^\\d{12}$");
        when(catalogoRepository.findByIdProductoAndActivoTrue(2)).thenReturn(Optional.of(p));

        ValidacionReferenciaResponse r = service.validarReferencia(solicitud(p, " 123456789012 ", null));

        assertThat(r.isValida()).isTrue();
        assertThat(r.getReferencia()).isEqualTo("123456789012");
    }

    @Test
    @DisplayName("Referencia fuera de formato o vacía es inválida, sin lanzar excepción")
    void referenciaInvalida() {
        CatalogoProductoPuntoRed p = producto(2, 1, "^\\d{12}$");
        when(catalogoRepository.findByIdProductoAndActivoTrue(2)).thenReturn(Optional.of(p));

        assertThat(service.validarReferencia(solicitud(p, "ABC", null)).isValida()).isFalse();
        assertThat(service.validarReferencia(solicitud(p, "  ", null)).isValida()).isFalse();
    }

    @Test
    @DisplayName("Recarga exige teléfono de 10 dígitos")
    void recargaTelefono() {
        CatalogoProductoPuntoRed p = producto(1, 0, null);
        when(catalogoRepository.findByIdProductoAndActivoTrue(1)).thenReturn(Optional.of(p));

        assertThat(service.validarReferencia(solicitud(p, null, "5512345678")).isValida()).isTrue();
        assertThat(service.validarReferencia(solicitud(p, null, "55123")).isValida()).isFalse();
    }

    @Test
    @DisplayName("Una expresión mal escrita en BD no bloquea la validación")
    void regexInvalidaSeOmite() {
        CatalogoProductoPuntoRed p = producto(2, 1, "([");
        when(catalogoRepository.findByIdProductoAndActivoTrue(2)).thenReturn(Optional.of(p));

        assertThat(service.validarReferencia(solicitud(p, "123", null)).isValida()).isTrue();
    }

    @Test
    @DisplayName("Producto inexistente o con idServicio distinto responde 400")
    void productoNoPermitido() {
        CatalogoProductoPuntoRed p = producto(2, 1, null);
        when(catalogoRepository.findByIdProductoAndActivoTrue(2)).thenReturn(Optional.of(p));

        ValidarReferenciaRequest otroServicio = ValidarReferenciaRequest.builder().idProducto(2).idServicio(999).referencia("1").build();
        assertThatThrownBy(() -> service.validarReferencia(otroServicio)).isInstanceOf(ReglaNegocioException.class);
    }

    @Test
    @DisplayName("Historial: rango de fechas invertido responde 400 sin consultar")
    void historialRangoInvalido() {
        HistorialPagosRequest req = HistorialPagosRequest.builder()
                .fechaDesde(LocalDate.of(2026, 5, 2)).fechaHasta(LocalDate.of(2026, 5, 1)).build();

        assertThatThrownBy(() -> service.historial(req, USUARIO)).isInstanceOf(ReglaNegocioException.class);
        verify(pagoServicioRepository, never()).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    @DisplayName("Historial: pagina y enriquece con el nombre del producto")
    void historialEnriquecido() {
        PagoServicio pago = PagoServicio.builder().upc("UPC1").curp(CURP).idProducto(2).idServicio(102)
                .monto(new BigDecimal("100.00")).estatus(EstatusPago.EXITOSO).build();
        when(pagoServicioRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(pago), PageRequest.of(0, 10), 1));
        when(catalogoRepository.findByIdProductoIn(anyCollection())).thenReturn(List.of(producto(2, 1, null)));

        PageResponse<PagoResponse> page = service.historial(new HistorialPagosRequest(), USUARIO);

        assertThat(page.getTotalElementos()).isEqualTo(1);
        assertThat(page.getContenido().get(0).getNombreProducto()).isEqualTo("Producto 2");
    }

    @Test
    @DisplayName("Detalle de un folio de otro titular responde 404")
    void detalleAjeno() {
        PagoServicio ajeno = PagoServicio.builder().upc("UPC2").curp("OTRA800101HDFRRR01").idProducto(2).build();
        when(pagoServicioRepository.findByUpc("UPC2")).thenReturn(Optional.of(ajeno));

        assertThatThrownBy(() -> service.detalle("UPC2", USUARIO)).isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    @DisplayName("Detalle de un folio propio incluye el producto")
    void detallePropio() {
        PagoServicio propio = PagoServicio.builder().upc("UPC3").curp(CURP).idProducto(2).estatus(EstatusPago.PENDIENTE).build();
        when(pagoServicioRepository.findByUpc("UPC3")).thenReturn(Optional.of(propio));
        when(catalogoRepository.findByIdProducto(2)).thenReturn(Optional.of(producto(2, 1, null)));

        PagoResponse r = service.detalle("UPC3", USUARIO);

        assertThat(r.getEstatus()).isEqualTo(EstatusPago.PENDIENTE);
        assertThat(r.getNombreServicio()).isEqualTo("Servicio 2");
    }
}
