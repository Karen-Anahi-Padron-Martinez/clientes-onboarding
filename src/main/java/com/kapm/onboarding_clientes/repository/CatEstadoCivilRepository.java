package com.kapm.onboarding_clientes.repository;

import com.kapm.onboarding_clientes.model.catalogo.CatEstadoCivil;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CatEstadoCivilRepository extends JpaRepository<CatEstadoCivil, Long> {
    Optional<CatEstadoCivil> findByCodigoIgnoreCase(String codigo);
    Optional<CatEstadoCivil> findByDescripcionIgnoreCase(String descripcion);
}
