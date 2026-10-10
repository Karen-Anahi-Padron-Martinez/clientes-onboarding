package com.kapm.onboarding_clientes.repository;

import com.kapm.onboarding_clientes.model.catalogo.CatPais;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CatPaisRepository extends JpaRepository<CatPais, Long> {
    Optional<CatPais> findByCodigoIgnoreCase(String codigo);
    Optional<CatPais> findByDescripcionIgnoreCase(String descripcion);
}
