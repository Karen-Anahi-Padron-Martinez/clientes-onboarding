package com.kapm.onboarding_clientes.repository;

import com.kapm.onboarding_clientes.model.catalogo.CatOcupacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CatOcupacionRepository extends JpaRepository<CatOcupacion, Long> {
    Optional<CatOcupacion> findByCodigoIgnoreCase(String codigo);
    Optional<CatOcupacion> findByDescripcionIgnoreCase(String descripcion);
}
