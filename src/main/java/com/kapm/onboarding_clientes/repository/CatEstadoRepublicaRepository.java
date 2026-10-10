package com.kapm.onboarding_clientes.repository;

import com.kapm.onboarding_clientes.model.catalogo.CatEstadoRepublica;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CatEstadoRepublicaRepository extends JpaRepository<CatEstadoRepublica, Long> {
    Optional<CatEstadoRepublica> findByCodigoIgnoreCase(String codigo);
    Optional<CatEstadoRepublica> findByDescripcionIgnoreCase(String descripcion);
}
