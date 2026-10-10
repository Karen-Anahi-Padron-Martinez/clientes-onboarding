package com.kapm.onboarding_clientes.repository;

import com.kapm.onboarding_clientes.model.catalogo.CatNacionalidad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CatNacionalidadRepository extends JpaRepository<CatNacionalidad, Long> {
    Optional<CatNacionalidad> findByCodigoIgnoreCase(String codigo);
    Optional<CatNacionalidad> findByDescripcionIgnoreCase(String descripcion);
}
