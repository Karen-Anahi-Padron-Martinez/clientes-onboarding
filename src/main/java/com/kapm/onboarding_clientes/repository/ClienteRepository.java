package com.kapm.onboarding_clientes.repository;

import com.kapm.onboarding_clientes.model.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    Optional<Cliente> findByCurp(String curp);

    Optional<Cliente> findByRfc(String rfc);

    Optional<Cliente> findByCorreo(String correo);

    Optional<Cliente> findByCuentaNumeroCuenta(String numeroCuenta);

    List<Cliente> findByActivoTrue();

    List<Cliente> findByFechaRegistroBetween(LocalDateTime fechaInicio, LocalDateTime fechaFin);

    boolean existsByCurp(String curp);

    boolean existsByRfc(String rfc);

    boolean existsByCorreo(String correo);

    @Query("SELECT c FROM Cliente c WHERE c.activo = true AND c.cuenta.estatus = 'ACTIVA'")
    List<Cliente> findClientesActivosConCuentaActiva();
}
