package com.kapm.onboarding_clientes.util;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * Cálculo de tiempo transcurrido sobre las fechas locales que persiste la aplicación.
 * Se resuelven en la zona del servidor (la misma de {@code LocalDateTime.now()} al guardarlas) para que un cambio
 * de horario de verano no altere la duración.
 */
public final class Tiempo {

    private Tiempo() {
    }

    public static Duration transcurridoDesde(LocalDateTime instante) {
        ZoneId zona = ZoneId.systemDefault();
        return Duration.between(instante.atZone(zona), ZonedDateTime.now(zona));
    }
}
