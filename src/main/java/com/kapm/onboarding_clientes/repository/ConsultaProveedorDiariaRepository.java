package com.kapm.onboarding_clientes.repository;

import com.kapm.onboarding_clientes.entity.ConsultaProveedorDiaria;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface ConsultaProveedorDiariaRepository extends JpaRepository<ConsultaProveedorDiaria, Long> {

    /** Bloqueo pesimista: dos instancias no pueden consumir el mismo cupo a la vez. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM ConsultaProveedorDiaria c WHERE c.operacion = :operacion AND c.fecha = :fecha")
    Optional<ConsultaProveedorDiaria> findByOperacionAndFechaWithLock(@Param("operacion") String operacion,
                                                                      @Param("fecha") LocalDate fecha);

    Optional<ConsultaProveedorDiaria> findTopByOperacionAndUltimaExitosaIsNotNullOrderByUltimaExitosaDesc(String operacion);
}
