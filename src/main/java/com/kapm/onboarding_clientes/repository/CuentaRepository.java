package com.kapm.onboarding_clientes.repository;

import com.kapm.onboarding_clientes.entity.Cuenta;
import com.kapm.onboarding_clientes.entity.EstatusCuenta;
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
