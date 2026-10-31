package com.kapm.onboarding_clientes.repository;

import com.kapm.onboarding_clientes.entity.EstatusPago;
import com.kapm.onboarding_clientes.entity.PagoServicio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio Spring Data JPA para la entidad PagoServicio.
 */
@Repository
public interface PagoServicioRepository extends JpaRepository<PagoServicio, Long>, JpaSpecificationExecutor<PagoServicio> {

    /**
     * Busca un pago de servicio por su código único UPC.
     */
    Optional<PagoServicio> findByUpc(String upc);

    /**
     * Verifica la existencia de un código UPC para garantizar unicidad al generar transacciones.
     */
    boolean existsByUpc(String upc);

    /**
     * Busca un pago de servicio por su llave de idempotencia.
     */
    Optional<PagoServicio> findByIdempotencyKey(String idempotencyKey);

    /**
     * Verifica la existencia de una llave de idempotencia para evitar cobros duplicados.
     */
    boolean existsByIdempotencyKey(String idempotencyKey);

    /**
     * Consulta el historial de pagos asociados a una CURP de cliente.
     */
    List<PagoServicio> findByCurp(String curp);

    /**
     * Consulta el historial de pagos asociados a un número de cuenta bancaria.
     */
    List<PagoServicio> findByNumeroCuenta(String numeroCuenta);

    /**
     * Consulta pagos filtrados por su estatus actual.
     */
    List<PagoServicio> findByEstatus(EstatusPago estatus);

    /**
     * Pagos en un estatus creados antes de cierto instante (insumo del scheduler de conciliación).
     */
    List<PagoServicio> findByEstatusAndFechaCreacionBefore(EstatusPago estatus, java.time.LocalDateTime limite);
}
