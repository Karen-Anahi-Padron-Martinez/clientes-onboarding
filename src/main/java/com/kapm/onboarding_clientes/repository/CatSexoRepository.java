package com.kapm.onboarding_clientes.repository;

import com.kapm.onboarding_clientes.model.catalogo.CatSexo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CatSexoRepository extends JpaRepository<CatSexo, Long> {
    Optional<CatSexo> findByCodigoIgnoreCase(String codigo);
    Optional<CatSexo> findByDescripcionIgnoreCase(String descripcion);
}
