package com.kapm.onboarding_clientes.repository;

import com.kapm.onboarding_clientes.entity.catalogo.CatalogoProductoPuntoRed;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio Spring Data JPA para el catálogo local de productos de PuntoRed.
 */
@Repository
public interface CatalogoProductoPuntoRedRepository extends JpaRepository<CatalogoProductoPuntoRed, Long> {

    /**
     * Busca un producto por su identificador oficial en PuntoRed.
     */
    Optional<CatalogoProductoPuntoRed> findByIdProducto(Integer idProducto);

    /**
     * Busca un producto por su identificador oficial que además se encuentre activo.
     */
    Optional<CatalogoProductoPuntoRed> findByIdProductoAndActivoTrue(Integer idProducto);

    /**
     * Obtiene todos los productos habilitados para venta.
     */
    List<CatalogoProductoPuntoRed> findByActivoTrue();

    /**
     * Verifica la existencia de un producto en el catálogo local.
     */
    boolean existsByIdProducto(Integer idProducto);
}
