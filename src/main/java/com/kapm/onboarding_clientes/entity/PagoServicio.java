package com.kapm.onboarding_clientes.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;


@Entity
@Table(name = "pagos_servicios", indexes = {
    @Index(name = "idx_pagos_servicios_upc", columnList = "upc", unique = true),
    @Index(name = "idx_pagos_servicios_idempotency", columnList = "idempotency_key", unique = true),
    @Index(name = "idx_pagos_servicios_curp", columnList = "curp"),
    @Index(name = "idx_pagos_servicios_cuenta", columnList = "numero_cuenta"),
    @Index(name = "idx_pagos_servicios_estatus", columnList = "estatus")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PagoServicio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    
    @Column(name = "upc", nullable = false, unique = true, updatable = false, columnDefinition = "TEXT")
    private String upc;

    @Column(name = "idempotency_key", unique = true, columnDefinition = "TEXT")
    private String idempotencyKey;

    @Column(name = "curp", columnDefinition = "TEXT")
    private String curp;

    @Column(name = "rfc", columnDefinition = "TEXT")
    private String rfc;

    @Column(name = "numero_cuenta", columnDefinition = "TEXT")
    private String numeroCuenta;

    @Column(name = "id_user", columnDefinition = "TEXT")
    private String idUser;


    @Column(name = "id_producto", nullable = false)
    private Integer idProducto;

    @Column(name = "id_servicio", nullable = false)
    private Integer idServicio;

    @Column(name = "telefono", nullable = false, columnDefinition = "TEXT")
    private String telefono;

    @Column(name = "referencia", columnDefinition = "TEXT")
    private String referencia;

    @Column(name = "monto", nullable = false, precision = 15, scale = 2)
    private BigDecimal monto;

    @Column(name = "comision", precision = 15, scale = 2)
    private BigDecimal comision;

    @Column(name = "hora_local", nullable = false, columnDefinition = "TEXT")
    private String horaLocal;

    @Column(name = "unidad", nullable = false, columnDefinition = "TEXT")
    private String unidad;

    @Enumerated(EnumType.STRING)
    @Column(name = "estatus", nullable = false, columnDefinition = "TEXT")
    private EstatusPago estatus;

    @Column(name = "id_tx_proveedor", columnDefinition = "TEXT")
    private String idTxProveedor;

    @Column(name = "num_autorizacion", columnDefinition = "TEXT")
    private String numAutorizacion;

    @Column(name = "codigo_respuesta", columnDefinition = "TEXT")
    private String codigoRespuesta;

    @Column(name = "texto_respuesta", columnDefinition = "TEXT")
    private String textoRespuesta;

    /** Intentos de verificación automática con el proveedor (confirmTx o reenvío del mismo UPC). */
    @Column(name = "intentos_verificacion")
    private Integer intentosVerificacion;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    @PrePersist
    protected void onCreate() {
        this.fechaCreacion = LocalDateTime.now();
        if (this.estatus == null) {
            this.estatus = EstatusPago.PENDIENTE;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.fechaActualizacion = LocalDateTime.now();
    }
}
