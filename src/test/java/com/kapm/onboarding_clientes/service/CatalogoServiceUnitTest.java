package com.kapm.onboarding_clientes.service;

import com.kapm.onboarding_clientes.dto.response.CatalogosResponse;
import com.kapm.onboarding_clientes.entity.catalogo.*;
import com.kapm.onboarding_clientes.exception.ReglaNegocioException;
import com.kapm.onboarding_clientes.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("CatalogoServiceImpl (unitarias): carga inicial, listados y normalización")
class CatalogoServiceUnitTest {

    @Mock CatSexoRepository sexoRepo;
    @Mock CatEstadoCivilRepository estadoCivilRepo;
    @Mock CatNacionalidadRepository nacionalidadRepo;
    @Mock CatOcupacionRepository ocupacionRepo;
    @Mock CatEstadoRepublicaRepository estadoRepo;
    @Mock CatPaisRepository paisRepo;
    @InjectMocks CatalogoServiceImpl servicio;

    @BeforeEach
    void guardarDevuelveEntidad() {
        when(sexoRepo.save(any())).then(returnsFirstArg());
        when(estadoCivilRepo.save(any())).then(returnsFirstArg());
        when(nacionalidadRepo.save(any())).then(returnsFirstArg());
        when(ocupacionRepo.save(any())).then(returnsFirstArg());
        when(estadoRepo.save(any())).then(returnsFirstArg());
        when(paisRepo.save(any())).then(returnsFirstArg());
    }

    @Test
    @DisplayName("La carga inicial solo puebla los catálogos vacíos")
    void cargaInicial() {
        when(sexoRepo.count()).thenReturn(3L);
        when(estadoCivilRepo.count()).thenReturn(5L);
        when(nacionalidadRepo.count()).thenReturn(1L);
        when(ocupacionRepo.count()).thenReturn(1L);
        when(estadoRepo.count()).thenReturn(1L);
        when(paisRepo.count()).thenReturn(1L);
        servicio.inicializarCatalogosSiEstanVacios();
        verify(sexoRepo, never()).save(any());
        verify(paisRepo, never()).save(any());

        when(sexoRepo.count()).thenReturn(0L);
        when(estadoCivilRepo.count()).thenReturn(0L);
        when(nacionalidadRepo.count()).thenReturn(0L);
        when(ocupacionRepo.count()).thenReturn(0L);
        when(estadoRepo.count()).thenReturn(0L);
        when(paisRepo.count()).thenReturn(0L);
        servicio.inicializarCatalogosSiEstanVacios();
        verify(sexoRepo, times(SexoCatalogo.values().length)).save(any());
        verify(paisRepo, times(PaisCatalogo.values().length)).save(any());
    }

    @Test
    @DisplayName("Los listados usan la BD si tiene datos y el enum si está vacía")
    void listados() {
        CatalogosResponse desdeEnum = servicio.obtenerTodosLosCatalogos();
        assertThat(desdeEnum.getSexos()).hasSize(SexoCatalogo.values().length);
        assertThat(desdeEnum.getEstadosRepublica()).hasSize(EstadoRepublicaCatalogo.values().length);
        assertThat(desdeEnum.getTiposBiometria()).extracting("descripcion").contains("Escaneo de Iris");
        assertThat(desdeEnum.getEstatusCuenta()).extracting("descripcion").containsExactly("Activa", "Inactiva", "Bloqueada");

        when(sexoRepo.findAll()).thenReturn(List.of(CatSexo.builder().codigo("X").descripcion("x").build()));
        when(estadoCivilRepo.findAll()).thenReturn(List.of(CatEstadoCivil.builder().codigo("X").build()));
        when(nacionalidadRepo.findAll()).thenReturn(List.of(CatNacionalidad.builder().codigo("X").build()));
        when(ocupacionRepo.findAll()).thenReturn(List.of(CatOcupacion.builder().codigo("X").build()));
        when(estadoRepo.findAll()).thenReturn(List.of(CatEstadoRepublica.builder().codigo("X").build()));
        when(paisRepo.findAll()).thenReturn(List.of(CatPais.builder().codigo("X").build()));
        CatalogosResponse desdeBd = servicio.obtenerTodosLosCatalogos();
        assertThat(desdeBd.getSexos()).singleElement().extracting("codigo").isEqualTo("X");
        assertThat(desdeBd.getEstadosCiviles()).hasSize(1);
        assertThat(desdeBd.getNacionalidades()).hasSize(1);
        assertThat(desdeBd.getOcupaciones()).hasSize(1);
        assertThat(desdeBd.getEstadosRepublica()).hasSize(1);
        assertThat(desdeBd.getPaises()).hasSize(1);
    }

    @Test
    @DisplayName("Sexo: obligatorio, búsqueda en BD, respaldo en enum y valor desconocido")
    void sexo() {
        assertThatThrownBy(() -> servicio.buscarSexo(null)).isInstanceOf(ReglaNegocioException.class);
        assertThatThrownBy(() -> servicio.buscarSexo(" ")).isInstanceOf(ReglaNegocioException.class);
        CatSexo bd = CatSexo.builder().codigo("FEMENINO").build();
        when(sexoRepo.findByCodigoIgnoreCase("FEMENINO")).thenReturn(Optional.of(bd));
        assertThat(servicio.buscarSexo("femenino")).isSameAs(bd);
        when(sexoRepo.findByDescripcionIgnoreCase("Hombre")).thenReturn(Optional.of(bd));
        assertThat(servicio.buscarSexo("Hombre")).isSameAs(bd);
        assertThat(servicio.normalizarSexo("masculino")).isEqualTo("MASCULINO");
        assertThat(servicio.buscarSexo("otro / no binario").getCodigo()).isEqualTo("OTRO");
        assertThatThrownBy(() -> servicio.buscarSexo("desconocido")).isInstanceOf(ReglaNegocioException.class);
    }

    @Test
    @DisplayName("Estado civil: obligatorio, respaldo por código o descripción y valor desconocido")
    void estadoCivil() {
        assertThatThrownBy(() -> servicio.buscarEstadoCivil(null)).isInstanceOf(ReglaNegocioException.class);
        assertThatThrownBy(() -> servicio.buscarEstadoCivil("")).isInstanceOf(ReglaNegocioException.class);
        assertThat(servicio.normalizarEstadoCivil("union libre")).isEqualTo("UNION_LIBRE");
        assertThat(servicio.buscarEstadoCivil("Soltero / a").getCodigo()).isEqualTo("SOLTERO");
        assertThatThrownBy(() -> servicio.buscarEstadoCivil("comprometido")).isInstanceOf(ReglaNegocioException.class);
    }

    @Test
    @DisplayName("Nacionalidad: Mexicana por defecto y registro personalizado si no existe")
    void nacionalidad() {
        assertThat(servicio.normalizarNacionalidad(null)).isEqualTo("Mexicana");
        assertThat(servicio.normalizarNacionalidad(" ")).isEqualTo("Mexicana");
        assertThat(servicio.buscarNacionalidad("estadounidense").getCodigo()).isEqualTo("ESTADOUNIDENSE");
        assertThat(servicio.buscarNacionalidad("Española").getCodigo()).isEqualTo("ESPANYOLA");
        assertThat(servicio.buscarNacionalidad("Sueca nórdica").getCodigo()).isEqualTo("SUECA_NÓRDICA");
    }

    @Test
    @DisplayName("Ocupación: obligatoria, respaldo en enum y registro personalizado")
    void ocupacion() {
        assertThatThrownBy(() -> servicio.buscarOcupacion(null)).isInstanceOf(ReglaNegocioException.class);
        assertThatThrownBy(() -> servicio.buscarOcupacion(" ")).isInstanceOf(ReglaNegocioException.class);
        assertThat(servicio.buscarOcupacion("estudiante").getCodigo()).isEqualTo("ESTUDIANTE");
        assertThat(servicio.normalizarOcupacion("Jubilado / Pensionado")).isEqualTo("Jubilado / Pensionado");
        assertThat(servicio.buscarOcupacion("Astronauta").getCodigo()).isEqualTo("ASTRONAUTA");
    }

    @Test
    @DisplayName("Estado de la república: obligatorio, sin acentos, descripción y registro personalizado")
    void estadoRepublica() {
        assertThatThrownBy(() -> servicio.buscarEstadoRepublica(null)).isInstanceOf(ReglaNegocioException.class);
        assertThatThrownBy(() -> servicio.buscarEstadoRepublica(" ")).isInstanceOf(ReglaNegocioException.class);
        assertThat(servicio.buscarEstadoRepublica("Nuevo León").getCodigo()).isEqualTo("NUEVO_LEON");
        assertThat(servicio.buscarEstadoRepublica("ciudad de méxico").getCodigo()).isEqualTo("CIUDAD_DE_MEXICO");
        assertThat(servicio.normalizarEstadoRepublica("Texas")).isEqualTo("Texas");
    }

    @Test
    @DisplayName("País: México por defecto, sin acentos y registro personalizado")
    void pais() {
        assertThat(servicio.normalizarPais(null)).isEqualTo("México");
        assertThat(servicio.normalizarPais("")).isEqualTo("México");
        assertThat(servicio.buscarPais("Canadá").getCodigo()).isEqualTo("CANADA");
        assertThat(servicio.buscarPais("Otro país").getCodigo()).isEqualTo("OTRO");
        assertThat(servicio.buscarPais("Japón").getCodigo()).isEqualTo("JAPON");
    }
}
