package com.kapm.onboarding_clientes.repository;

import com.kapm.onboarding_clientes.model.Cuenta;
import com.kapm.onboarding_clientes.model.EstatusCuenta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CuentaRepository extends JpaRepository<Cuenta, Long> {

    Optional<Cuenta> findByNumeroCuenta(String numeroCuenta);

    List<Cuenta> findByEstatus(EstatusCuenta estatus);

    boolean existsByNumeroCuenta(String numeroCuenta);
}
