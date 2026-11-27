package com.kapm.onboarding_clientes.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MensajeProveedor: texto de PuntoRed sin volcados técnicos")
class MensajeProveedorTest {

    @Test
    @DisplayName("El rechazo por IP no autorizada no expone IPs ni dispositivo")
    void rechazoPorIp() {
        String volcado = "{validationCompleted=false, codigoDispositivo=GPS83-TPV-17, IP=148.224.32.25, isValid=false, "
                + "validIPS=(83): , (83) [163.192.134.186],[44.205.98.58]}";
        String limpio = MensajeProveedor.limpiar(volcado);
        assertThat(limpio).doesNotContain("148.224").doesNotContain("GPS83").doesNotContain("{");
        assertThat(limpio).startsWith("Operación no autorizada por el proveedor");
    }

    @Test
    @DisplayName("Si el volcado trae message=..., se muestra ese mensaje del proveedor")
    void mensajeDelProveedor() {
        String volcado = "{validationCompleted=false, codigoDispositivo=GPS83-TPV-17, IP=148.224.32.25, isValid=false, "
                + "validIPS=(83): , (83) [163.192.134.186],[20.165.0.155],201.137.8.145, message=Origen Valido, idDistribuidor=83}";
        assertThat(MensajeProveedor.limpiar(volcado)).isEqualTo("Origen Valido");
    }

    @Test
    @DisplayName("Conserva el mensaje legible y corta lo técnico")
    void conservaMensaje() {
        assertThat(MensajeProveedor.limpiar("TRANSACCION APROBADA")).isEqualTo("TRANSACCION APROBADA");
        assertThat(MensajeProveedor.limpiar("Referencia invalida: {detalle=abc}")).isEqualTo("Referencia invalida");
        assertThat(MensajeProveedor.limpiar("Saldo insuficiente codigoError=77")).isEqualTo("Saldo insuficiente");
        assertThat(MensajeProveedor.limpiar("{a=1}")).isEqualTo("Operación rechazada por el proveedor");
        assertThat(MensajeProveedor.limpiar(null)).isNull();
        assertThat(MensajeProveedor.limpiar("x".repeat(300))).hasSizeLessThanOrEqualTo(121);
    }
}
