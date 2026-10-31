package com.kapm.onboarding_clientes.client.puntored.model;

/**
 * Resultado interpretado de confirmTx.do.
 *
 * @param estado    estado de la transacción en PuntoRed
 * @param respuesta respuesta cruda (número de autorización e ID_TX cuando fue aplicada)
 */
public record ConfirmacionTx(Estado estado, PuntoRedTxResponse respuesta) {

    public enum Estado {
        /** CODIGO 01: la transacción se aplicó. */
        APLICADA,
        /** CODIGO 03: PuntoRed no registró la transacción. */
        NO_ENCONTRADA,
        /** CODIGO 04 o cualquier código no documentado: aún no hay certeza. */
        PENDIENTE
    }
}
