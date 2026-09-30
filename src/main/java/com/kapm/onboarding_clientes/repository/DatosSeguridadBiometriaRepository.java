package com.kapm.onboarding_clientes.repository;

import com.kapm.onboarding_clientes.model.DatosSeguridadBiometria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DatosSeguridadBiometriaRepository extends JpaRepository<DatosSeguridadBiometria, Long> {

    Optional<DatosSeguridadBiometria> findByUsername(String username);

    boolean existsByUsername(String username);
}
