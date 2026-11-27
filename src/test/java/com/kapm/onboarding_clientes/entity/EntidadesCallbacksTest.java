package com.kapm.onboarding_clientes.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Callbacks @PrePersist/@PreUpdate de las entidades")
class EntidadesCallbacksTest {

    @Test
    @DisplayName("Cliente: activo por defecto sin sobrescribir un valor explícito")
    void cliente() {
        Cliente nuevo = new Cliente();
        nuevo.onCreate();
        assertThat(nuevo.getActivo()).isTrue();
        assertThat(nuevo.getFechaRegistro()).isNotNull();

        Cliente inactivo = new Cliente();
        inactivo.setActivo(false);
        inactivo.onCreate();
        assertThat(inactivo.getActivo()).isFalse();
    }

    @Test
    @DisplayName("PagoServicio: estatus PENDIENTE por defecto y fecha de actualización")
    void pagoServicio() {
        PagoServicio nuevo = new PagoServicio();
        nuevo.onCreate();
        assertThat(nuevo.getEstatus()).isEqualTo(EstatusPago.PENDIENTE);
        assertThat(nuevo.getFechaCreacion()).isNotNull();

        PagoServicio exitoso = new PagoServicio();
        exitoso.setEstatus(EstatusPago.EXITOSO);
        exitoso.onCreate();
        exitoso.onUpdate();
        assertThat(exitoso.getEstatus()).isEqualTo(EstatusPago.EXITOSO);
        assertThat(exitoso.getFechaActualizacion()).isNotNull();
    }

    @Test
    @DisplayName("DatosSeguridadBiometria: loggeado en false por defecto")
    void datosSeguridad() {
        DatosSeguridadBiometria nuevo = new DatosSeguridadBiometria();
        nuevo.onCreate();
        assertThat(nuevo.getLoggeado()).isFalse();
        assertThat(nuevo.getFechaRegistroBiometrico()).isNotNull();

        DatosSeguridadBiometria loggeado = new DatosSeguridadBiometria();
        loggeado.setLoggeado(true);
        loggeado.onCreate();
        assertThat(loggeado.getLoggeado()).isTrue();
    }

    @Test
    @DisplayName("CodigoVerificacionPago: completa solo los valores ausentes")
    void codigoVerificacion() {
        CodigoVerificacionPago vacio = new CodigoVerificacionPago();
        vacio.setIntentos(null);
        vacio.setUsado(null);
        vacio.onCreate();
        assertThat(vacio.getCreadoEn()).isNotNull();
        assertThat(vacio.getIntentos()).isZero();
        assertThat(vacio.getUsado()).isFalse();

        LocalDateTime creado = LocalDateTime.of(2026, 1, 1, 10, 0);
        CodigoVerificacionPago completo = new CodigoVerificacionPago();
        completo.setCreadoEn(creado);
        completo.setIntentos(2);
        completo.setUsado(true);
        completo.onCreate();
        assertThat(completo.getCreadoEn()).isEqualTo(creado);
        assertThat(completo.getIntentos()).isEqualTo(2);
        assertThat(completo.getUsado()).isTrue();
    }
}
