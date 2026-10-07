package com.kapm.onboarding_clientes.service;

import com.kapm.onboarding_clientes.dto.response.CatalogoItemDTO;
import com.kapm.onboarding_clientes.dto.response.CatalogosResponse;
import com.kapm.onboarding_clientes.model.catalogo.*;

import java.util.List;

public interface CatalogoService {
    CatalogosResponse obtenerTodosLosCatalogos();
    List<CatalogoItemDTO> obtenerSexos();
    List<CatalogoItemDTO> obtenerEstadosCiviles();
    List<CatalogoItemDTO> obtenerNacionalidades();
    List<CatalogoItemDTO> obtenerOcupaciones();
    List<CatalogoItemDTO> obtenerEstadosRepublica();
    List<CatalogoItemDTO> obtenerPaises();
    List<CatalogoItemDTO> obtenerTiposBiometria();
    List<CatalogoItemDTO> obtenerEstatusCuenta();

    // Búsqueda de entidades de catálogo para relaciones JPA (Foreign Keys)
    CatSexo buscarSexo(String valor);
    CatEstadoCivil buscarEstadoCivil(String valor);
    CatNacionalidad buscarNacionalidad(String valor);
    CatOcupacion buscarOcupacion(String valor);
    CatEstadoRepublica buscarEstadoRepublica(String valor);
    CatPais buscarPais(String valor);

    // Normalización de texto
    String normalizarSexo(String sexo);
    String normalizarEstadoCivil(String estadoCivil);
    String normalizarNacionalidad(String nacionalidad);
    String normalizarOcupacion(String ocupacion);
    String normalizarEstadoRepublica(String estado);
    String normalizarPais(String pais);
}
