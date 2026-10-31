package com.kapm.onboarding_clientes.service;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Control persistente del cupo diario de operaciones limitadas por el proveedor.
 * Cada método corre en su propia transacción para que el consumo de cupo quede registrado aunque la llamada
 * HTTP posterior falle o la transacción del llamador se revierta.
 */
public interface CuotaConsultaProveedorService {

    /**
     * Consume una unidad del cupo del día si queda disponible.
     *
     * @return false si el cupo del día ya se agotó (la llamada NO debe hacerse)
     */
    boolean reservarConsulta(String operacion, int limiteDiario);

    void registrarExito(String operacion);

    Optional<LocalDateTime> ultimaConsultaExitosa(String operacion);
}
