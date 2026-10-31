package com.kapm.onboarding_clientes.scheduler;

import com.kapm.onboarding_clientes.service.CatalogoPuntoRedSyncService;
import com.kapm.onboarding_clientes.service.VerificacionPagosService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Tareas periódicas de la integración con PuntoRed. Solo orquesta; la lógica vive en los servicios.
 * Se desactiva con {@code puntored.scheduler-enabled=false} (p. ej. en pruebas).
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "puntored", name = "scheduler-enabled", havingValue = "true", matchIfMissing = true)
public class PuntoRedScheduler {

    private final VerificacionPagosService verificacionPagosService;
    private final CatalogoPuntoRedSyncService catalogoSyncService;

    @Scheduled(fixedDelayString = "${puntored.conciliacion-intervalo-ms:120000}", initialDelayString = "${puntored.conciliacion-intervalo-ms:120000}")
    public void verificarPagosSinConfirmar() {
        int resueltos = verificacionPagosService.verificarPendientes();
        if (resueltos > 0) {
            log.info("Verificación con PuntoRed: {} pago(s) con resultado desconocido cambiaron de estado", resueltos);
        }
    }

    @EventListener(ApplicationReadyEvent.class)
    public void sincronizarCatalogoAlIniciar() {
        sincronizarCatalogo();
    }

    @Scheduled(cron = "${puntored.catalogo-sync-cron:0 0 4 * * *}", zone = "America/Mexico_City")
    public void sincronizarCatalogo() {
        try {
            catalogoSyncService.sincronizar(false);
        } catch (RuntimeException e) {
            // Sin catálogo actualizado se sigue operando con el local; nunca se tumba el arranque
            log.error("Falló la sincronización del catálogo PuntoRed: {}", e.getClass().getSimpleName());
        }
    }
}
