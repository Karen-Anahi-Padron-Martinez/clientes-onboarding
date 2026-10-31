package com.kapm.onboarding_clientes.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "puntored")
public class PuntoRedProperties {

    private String baseUrl;
    private String authorizationUrl;
    private String usuario;
    private String idDistribuidor;
    private String codigoDispositivo;
    private String password;
    private String unidad;
    private String zonaHoraria = "America/Mexico_City";
    private String authPath = "/sistema/app/jwt-gp/authenticate/";
    private String aesKey;
    private String aesIv;
    /** Header X-API-Key: PuntoRed lo exige solo si lo asignó al distribuidor. Vacío => no se envía. */
    private String apiKey;
    private int connectTimeout = 5000;
    /** PuntoRed espera hasta 62 s antes de responder CODIGO 82; cortar antes solo produce resultados desconocidos. */
    private int readTimeout = 70000;

    /** Límites globales por operación cuando el producto del catálogo no define los suyos. */
    private BigDecimal montoMinimoOperacion = new BigDecimal("1.00");
    private BigDecimal montoMaximoOperacion = new BigDecimal("5000.00");

    /**
     * Antigüedad mínima de un pago PENDIENTE antes de verificarlo: mayor que readTimeout para no competir
     * con la petición original que aún podría estar esperando respuesta.
     */
    private int minutosAntesDeVerificar = 2;

    /**
     * Ventanas en las que reenviar el mismo UPC es seguro porque PuntoRed lo detecta como duplicado
     * (15 min en recargas y 24 h en servicios, según su documentación). Se usan márgenes por debajo del límite.
     */
    private int minutosVentanaReenvioRecarga = 10;
    private int minutosVentanaReenvioServicio = 1380;

    /** confirmTx solo es válido 24 h después del sendTx. */
    private int minutosVigenciaConfirmTx = 1440;

    /** Intentos de verificación automática antes de escalar el pago a conciliación manual. */
    private int maxIntentosVerificacion = 5;

    /** PuntoRed limita getProductList.do a 3 llamadas diarias por distribuidor. */
    private int catalogoLimiteDiario = 3;

    /** Antigüedad mínima de la última sincronización exitosa para volver a consultar (evita gastar cupo en reinicios). */
    private int catalogoHorasEntreSincronizaciones = 20;

    /** Habilita las tareas programadas (conciliación y sincronización de catálogo). */
    private boolean schedulerEnabled = true;
    public String getUsername() {
        return usuario;
    }

    public void setUsername(String username) {
        this.usuario = username;
    }

    public Integer getIdDistribuidorAsInteger() {
        if (idDistribuidor == null || idDistribuidor.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(idDistribuidor.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
