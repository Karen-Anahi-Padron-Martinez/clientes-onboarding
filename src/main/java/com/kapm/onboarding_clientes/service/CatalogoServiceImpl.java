package com.kapm.onboarding_clientes.service;

import com.kapm.onboarding_clientes.dto.response.CatalogoItemDTO;
import com.kapm.onboarding_clientes.dto.response.CatalogosResponse;
import com.kapm.onboarding_clientes.exception.ReglaNegocioException;
import com.kapm.onboarding_clientes.model.EstatusCuenta;
import com.kapm.onboarding_clientes.model.TipoBiometria;
import com.kapm.onboarding_clientes.model.catalogo.*;
import com.kapm.onboarding_clientes.repository.*;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CatalogoServiceImpl implements CatalogoService {

    private final CatSexoRepository catSexoRepository;
    private final CatEstadoCivilRepository catEstadoCivilRepository;
    private final CatNacionalidadRepository catNacionalidadRepository;
    private final CatOcupacionRepository catOcupacionRepository;
    private final CatEstadoRepublicaRepository catEstadoRepublicaRepository;
    private final CatPaisRepository catPaisRepository;

    @PostConstruct
    @Transactional
    public void inicializarCatalogosSiEstanVacios() {
        if (catSexoRepository.count() == 0) {
            for (SexoCatalogo s : SexoCatalogo.values()) {
                catSexoRepository.save(CatSexo.builder().codigo(s.getCodigo()).descripcion(s.getDescripcion()).build());
            }
        }

        if (catEstadoCivilRepository.count() == 0) {
            for (EstadoCivilCatalogo e : EstadoCivilCatalogo.values()) {
                catEstadoCivilRepository.save(CatEstadoCivil.builder().codigo(e.getCodigo()).descripcion(e.getDescripcion()).build());
            }
        }

        if (catNacionalidadRepository.count() == 0) {
            for (NacionalidadCatalogo n : NacionalidadCatalogo.values()) {
                catNacionalidadRepository.save(CatNacionalidad.builder().codigo(n.getCodigo()).descripcion(n.getDescripcion()).build());
            }
        }

        if (catOcupacionRepository.count() == 0) {
            for (OcupacionCatalogo o : OcupacionCatalogo.values()) {
                catOcupacionRepository.save(CatOcupacion.builder().codigo(o.getCodigo()).descripcion(o.getDescripcion()).build());
            }
        }

        if (catEstadoRepublicaRepository.count() == 0) {
            for (EstadoRepublicaCatalogo e : EstadoRepublicaCatalogo.values()) {
                catEstadoRepublicaRepository.save(CatEstadoRepublica.builder().codigo(e.getCodigo()).descripcion(e.getDescripcion()).build());
            }
        }

        if (catPaisRepository.count() == 0) {
            for (PaisCatalogo p : PaisCatalogo.values()) {
                catPaisRepository.save(CatPais.builder().codigo(p.getCodigo()).descripcion(p.getDescripcion()).build());
            }
        }
    }

    @Override
    public CatalogosResponse obtenerTodosLosCatalogos() {
        return CatalogosResponse.builder()
                .sexos(obtenerSexos())
                .estadosCiviles(obtenerEstadosCiviles())
                .nacionalidades(obtenerNacionalidades())
                .ocupaciones(obtenerOcupaciones())
                .estadosRepublica(obtenerEstadosRepublica())
                .paises(obtenerPaises())
                .tiposBiometria(obtenerTiposBiometria())
                .estatusCuenta(obtenerEstatusCuenta())
                .build();
    }

    @Override
    public List<CatalogoItemDTO> obtenerSexos() {
        List<CatSexo> list = catSexoRepository.findAll();
        if (!list.isEmpty()) {
            return list.stream().map(s -> new CatalogoItemDTO(s.getCodigo(), s.getDescripcion())).collect(Collectors.toList());
        }
        return Arrays.stream(SexoCatalogo.values())
                .map(s -> new CatalogoItemDTO(s.getCodigo(), s.getDescripcion()))
                .collect(Collectors.toList());
    }

    @Override
    public List<CatalogoItemDTO> obtenerEstadosCiviles() {
        List<CatEstadoCivil> list = catEstadoCivilRepository.findAll();
        if (!list.isEmpty()) {
            return list.stream().map(e -> new CatalogoItemDTO(e.getCodigo(), e.getDescripcion())).collect(Collectors.toList());
        }
        return Arrays.stream(EstadoCivilCatalogo.values())
                .map(e -> new CatalogoItemDTO(e.getCodigo(), e.getDescripcion()))
                .collect(Collectors.toList());
    }

    @Override
    public List<CatalogoItemDTO> obtenerNacionalidades() {
        List<CatNacionalidad> list = catNacionalidadRepository.findAll();
        if (!list.isEmpty()) {
            return list.stream().map(n -> new CatalogoItemDTO(n.getCodigo(), n.getDescripcion())).collect(Collectors.toList());
        }
        return Arrays.stream(NacionalidadCatalogo.values())
                .map(n -> new CatalogoItemDTO(n.getCodigo(), n.getDescripcion()))
                .collect(Collectors.toList());
    }

    @Override
    public List<CatalogoItemDTO> obtenerOcupaciones() {
        List<CatOcupacion> list = catOcupacionRepository.findAll();
        if (!list.isEmpty()) {
            return list.stream().map(o -> new CatalogoItemDTO(o.getCodigo(), o.getDescripcion())).collect(Collectors.toList());
        }
        return Arrays.stream(OcupacionCatalogo.values())
                .map(o -> new CatalogoItemDTO(o.getCodigo(), o.getDescripcion()))
                .collect(Collectors.toList());
    }

    @Override
    public List<CatalogoItemDTO> obtenerEstadosRepublica() {
        List<CatEstadoRepublica> list = catEstadoRepublicaRepository.findAll();
        if (!list.isEmpty()) {
            return list.stream().map(e -> new CatalogoItemDTO(e.getCodigo(), e.getDescripcion())).collect(Collectors.toList());
        }
        return Arrays.stream(EstadoRepublicaCatalogo.values())
                .map(e -> new CatalogoItemDTO(e.getCodigo(), e.getDescripcion()))
                .collect(Collectors.toList());
    }

    @Override
    public List<CatalogoItemDTO> obtenerPaises() {
        List<CatPais> list = catPaisRepository.findAll();
        if (!list.isEmpty()) {
            return list.stream().map(p -> new CatalogoItemDTO(p.getCodigo(), p.getDescripcion())).collect(Collectors.toList());
        }
        return Arrays.stream(PaisCatalogo.values())
                .map(p -> new CatalogoItemDTO(p.getCodigo(), p.getDescripcion()))
                .collect(Collectors.toList());
    }

    @Override
    public List<CatalogoItemDTO> obtenerTiposBiometria() {
        return Arrays.stream(TipoBiometria.values())
                .map(t -> {
                    String descripcion;
                    switch (t) {
                        case HUELLA_DACTILAR -> descripcion = "Huella Dactilar";
                        case RECONOCIMIENTO_FACIAL -> descripcion = "Reconocimiento Facial";
                        case IRIS -> descripcion = "Escaneo de Iris";
                        case PATRON_VASCULAR -> descripcion = "Patrón Vascular";
                        default -> descripcion = t.name();
                    }
                    return new CatalogoItemDTO(t.name(), descripcion);
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<CatalogoItemDTO> obtenerEstatusCuenta() {
        return Arrays.stream(EstatusCuenta.values())
                .map(e -> {
                    String descripcion;
                    switch (e) {
                        case ACTIVA -> descripcion = "Activa";
                        case INACTIVA -> descripcion = "Inactiva";
                        case BLOQUEADA -> descripcion = "Bloqueada";
                        default -> descripcion = e.name();
                    }
                    return new CatalogoItemDTO(e.name(), descripcion);
                })
                .collect(Collectors.toList());
    }

    @Override
    public CatSexo buscarSexo(String valor) {
        if (valor == null || valor.isBlank()) {
            throw new ReglaNegocioException("El sexo es obligatorio");
        }
        String clean = valor.trim().toUpperCase();
        return catSexoRepository.findByCodigoIgnoreCase(clean)
                .or(() -> catSexoRepository.findByDescripcionIgnoreCase(valor.trim()))
                .orElseGet(() -> {
                    for (SexoCatalogo s : SexoCatalogo.values()) {
                        if (s.getCodigo().equalsIgnoreCase(clean) || s.getDescripcion().equalsIgnoreCase(valor.trim())) {
                            return catSexoRepository.save(CatSexo.builder().codigo(s.getCodigo()).descripcion(s.getDescripcion()).build());
                        }
                    }
                    throw new ReglaNegocioException("El valor de sexo '" + valor + "' no es válido según el catálogo oficial");
                });
    }

    @Override
    public CatEstadoCivil buscarEstadoCivil(String valor) {
        if (valor == null || valor.isBlank()) {
            throw new ReglaNegocioException("El estado civil es obligatorio");
        }
        String clean = valor.trim().toUpperCase().replace("/", "").replace(" ", "_");
        return catEstadoCivilRepository.findByCodigoIgnoreCase(clean)
                .or(() -> catEstadoCivilRepository.findByDescripcionIgnoreCase(valor.trim()))
                .orElseGet(() -> {
                    for (EstadoCivilCatalogo e : EstadoCivilCatalogo.values()) {
                        if (e.getCodigo().equalsIgnoreCase(clean) || e.name().equalsIgnoreCase(clean) || e.getDescripcion().equalsIgnoreCase(valor.trim())) {
                            return catEstadoCivilRepository.save(CatEstadoCivil.builder().codigo(e.getCodigo()).descripcion(e.getDescripcion()).build());
                        }
                    }
                    throw new ReglaNegocioException("El estado civil '" + valor + "' no es válido según el catálogo oficial");
                });
    }

    @Override
    public CatNacionalidad buscarNacionalidad(String valor) {
        String clean = (valor == null || valor.isBlank()) ? "Mexicana" : valor.trim();
        String cleanUpper = clean.toUpperCase();
        return catNacionalidadRepository.findByCodigoIgnoreCase(cleanUpper)
                .or(() -> catNacionalidadRepository.findByDescripcionIgnoreCase(clean))
                .orElseGet(() -> {
                    for (NacionalidadCatalogo n : NacionalidadCatalogo.values()) {
                        if (n.getCodigo().equalsIgnoreCase(cleanUpper) || n.getDescripcion().equalsIgnoreCase(clean)) {
                            return catNacionalidadRepository.save(CatNacionalidad.builder().codigo(n.getCodigo()).descripcion(n.getDescripcion()).build());
                        }
                    }
                    // Guardar como registro válido personalizado
                    return catNacionalidadRepository.save(CatNacionalidad.builder().codigo(cleanUpper.replace(" ", "_")).descripcion(clean).build());
                });
    }

    @Override
    public CatOcupacion buscarOcupacion(String valor) {
        if (valor == null || valor.isBlank()) {
            throw new ReglaNegocioException("La ocupación es obligatoria");
        }
        String clean = valor.trim();
        String cleanUpper = clean.toUpperCase().replace(" ", "_");
        return catOcupacionRepository.findByCodigoIgnoreCase(cleanUpper)
                .or(() -> catOcupacionRepository.findByDescripcionIgnoreCase(clean))
                .orElseGet(() -> {
                    for (OcupacionCatalogo o : OcupacionCatalogo.values()) {
                        if (o.getCodigo().equalsIgnoreCase(cleanUpper) || o.getDescripcion().equalsIgnoreCase(clean)) {
                            return catOcupacionRepository.save(CatOcupacion.builder().codigo(o.getCodigo()).descripcion(o.getDescripcion()).build());
                        }
                    }
                    return catOcupacionRepository.save(CatOcupacion.builder().codigo(cleanUpper).descripcion(clean).build());
                });
    }

    @Override
    public CatEstadoRepublica buscarEstadoRepublica(String valor) {
        if (valor == null || valor.isBlank()) {
            throw new ReglaNegocioException("El estado del domicilio es obligatorio");
        }
        String clean = valor.trim();
        String cleanUpper = clean.toUpperCase()
                .replace("Á", "A").replace("É", "E").replace("Í", "I").replace("Ó", "O").replace("Ú", "U")
                .replace(" ", "_");

        return catEstadoRepublicaRepository.findByCodigoIgnoreCase(cleanUpper)
                .or(() -> catEstadoRepublicaRepository.findByDescripcionIgnoreCase(clean))
                .orElseGet(() -> {
                    for (EstadoRepublicaCatalogo e : EstadoRepublicaCatalogo.values()) {
                        String normName = e.name().replace("Á", "A").replace("É", "E").replace("Í", "I").replace("Ó", "O").replace("Ú", "U");
                        if (normName.equalsIgnoreCase(cleanUpper) || e.getCodigo().equalsIgnoreCase(cleanUpper) || e.getDescripcion().equalsIgnoreCase(clean)) {
                            return catEstadoRepublicaRepository.save(CatEstadoRepublica.builder().codigo(e.getCodigo()).descripcion(e.getDescripcion()).build());
                        }
                    }
                    return catEstadoRepublicaRepository.save(CatEstadoRepublica.builder().codigo(cleanUpper).descripcion(clean).build());
                });
    }

    @Override
    public CatPais buscarPais(String valor) {
        String clean = (valor == null || valor.isBlank()) ? "México" : valor.trim();
        String cleanUpper = clean.toUpperCase()
                .replace("Á", "A").replace("É", "E").replace("Í", "I").replace("Ó", "O").replace("Ú", "U")
                .replace(" ", "_");

        return catPaisRepository.findByCodigoIgnoreCase(cleanUpper)
                .or(() -> catPaisRepository.findByDescripcionIgnoreCase(clean))
                .orElseGet(() -> {
                    for (PaisCatalogo p : PaisCatalogo.values()) {
                        String normName = p.name().replace("Á", "A").replace("É", "E").replace("Í", "I").replace("Ó", "O").replace("Ú", "U");
                        if (normName.equalsIgnoreCase(cleanUpper) || p.getCodigo().equalsIgnoreCase(cleanUpper) || p.getDescripcion().equalsIgnoreCase(clean)) {
                            return catPaisRepository.save(CatPais.builder().codigo(p.getCodigo()).descripcion(p.getDescripcion()).build());
                        }
                    }
                    return catPaisRepository.save(CatPais.builder().codigo(cleanUpper).descripcion(clean).build());
                });
    }

    @Override
    public String normalizarSexo(String sexo) {
        return buscarSexo(sexo).getCodigo();
    }

    @Override
    public String normalizarEstadoCivil(String estadoCivil) {
        return buscarEstadoCivil(estadoCivil).getCodigo();
    }

    @Override
    public String normalizarNacionalidad(String nacionalidad) {
        return buscarNacionalidad(nacionalidad).getDescripcion();
    }

    @Override
    public String normalizarOcupacion(String ocupacion) {
        return buscarOcupacion(ocupacion).getDescripcion();
    }

    @Override
    public String normalizarEstadoRepublica(String estado) {
        return buscarEstadoRepublica(estado).getDescripcion();
    }

    @Override
    public String normalizarPais(String pais) {
        return buscarPais(pais).getDescripcion();
    }
}
