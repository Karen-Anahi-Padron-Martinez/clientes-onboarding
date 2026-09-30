package com.kapm.onboarding_clientes.repository;

import com.kapm.onboarding_clientes.model.Saldo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SaldoRepository extends JpaRepository<Saldo, Long> {

    Optional<Saldo> findByCuentaNumeroCuenta(String numeroCuenta);

    Optional<Saldo> findByCuentaId(Long cuentaId);
}
