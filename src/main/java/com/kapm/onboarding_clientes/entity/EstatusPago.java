package com.kapm.onboarding_clientes.entity;

public enum EstatusPago {
    PENDIENTE,
    EXITOSO,
    RECHAZADO,
    REVERSADO,
    TIMEOUT,
    /** Resultado desconocido tras agotar la ventana de espera: saldo sigue reservado hasta resolución manual. */
    EN_CONCILIACION
}
