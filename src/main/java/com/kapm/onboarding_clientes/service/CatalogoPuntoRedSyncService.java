package com.kapm.onboarding_clientes.service;

/**
 * Mantiene el catálogo local de productos sincronizado con getProductList.do de PuntoRed.
 * Las transacciones se validan contra este catálogo local, nunca contra datos enviados por el cliente.
 */
public interface CatalogoPuntoRedSyncService {

    /**
     * Inserta productos nuevos, actualiza nombres/servicio/tipoFront de los existentes y desactiva
     * los que PuntoRed ya no ofrece. Conserva los límites de monto definidos localmente por el negocio.
     * <p>
     * getProductList.do está limitado a 3 llamadas diarias: nunca se excede ese cupo. Sin {@code forzar},
     * tampoco se consulta si la última sincronización exitosa es reciente (protege el cupo ante reinicios).
     *
     * @param forzar true para ignorar la antigüedad mínima (operación manual del ADMIN); el cupo diario se respeta siempre
     * @return número de productos activos tras la sincronización
     * @throws com.kapm.onboarding_clientes.exception.ReglaNegocioException si es forzada y el cupo del día se agotó
     */
    int sincronizar(boolean forzar);
}
