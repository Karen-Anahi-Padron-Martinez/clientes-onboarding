package com.kapm.onboarding_clientes.repository;

import com.kapm.onboarding_clientes.entity.Saldo;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SaldoRepository extends JpaRepository<Saldo, Long> {

    Optional<Saldo> findByCuentaNumeroCuenta(String numeroCuenta);

    Optional<Saldo> findByCuentaId(Long cuentaId);

    /**
     * Bloqueo pesimista de escritura sobre el registro de Saldo para evitar condiciones de carrera
     * durante la reserva y liquidación concurrente de pagos de servicios.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Saldo s WHERE s.cuenta.numeroCuenta = :numeroCuenta")
    Optional<Saldo> findByCuentaNumeroCuentaWithLock(@Param("numeroCuenta") String numeroCuenta);
}
