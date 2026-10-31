package com.kapm.onboarding_clientes.repository;

import com.kapm.onboarding_clientes.entity.CodigoVerificacionPago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio Spring Data JPA para la entidad CodigoVerificacionPago.
 */
@Repository
public interface CodigoVerificacionPagoRepository extends JpaRepository<CodigoVerificacionPago, Long> {

    /**
     * Obtiene el código más reciente emitido para una cuenta (usado o no).
     */
    Optional<CodigoVerificacionPago> findTopByNumeroCuentaOrderByCreadoEnDesc(String numeroCuenta);

    /**
     * Obtiene el código activo más reciente emitido para una cuenta.
     */
    Optional<CodigoVerificacionPago> findTopByNumeroCuentaAndUsadoFalseOrderByCreadoEnDesc(String numeroCuenta);

    /**
     * Obtiene todos los códigos no utilizados de una cuenta para proceder a invalidarlos.
     */
    List<CodigoVerificacionPago> findByNumeroCuentaAndUsadoFalse(String numeroCuenta);

    /**
     * Cuenta la cantidad de códigos solicitados en una ventana de tiempo para control de rate limiting.
     */
    long countByNumeroCuentaAndCreadoEnAfter(String numeroCuenta, LocalDateTime fechaLimite);
}
