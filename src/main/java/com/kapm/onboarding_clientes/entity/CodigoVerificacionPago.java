package com.kapm.onboarding_clientes.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entidad que almacena los códigos de verificación de dos factores (2FA) para transacciones de pago.
 * <p>
 * Reglas de seguridad y arquitectura:
 * <ul>
 *   <li>El código de 6 dígitos se guarda EXCLUSIVAMENTE mediante su hash (BCrypt), NUNCA en texto plano.</li>
 *   <li>hashOperacion vincula el código de forma unívoca a los datos exactos de la transacción.</li>
 *   <li>Cumple con la directiva arquitectónica: columnas alfanuméricas en TEXT (sin VARCHAR).</li>
 * </ul>
 */
@Entity
@Table(name = "codigos_verificacion_pago", indexes = {
    @Index(name = "idx_codigos_verif_cuenta", columnList = "numero_cuenta"),
    @Index(name = "idx_codigos_verif_hash_op", columnList = "hash_operacion"),
    @Index(name = "idx_codigos_verif_creado", columnList = "creado_en"),
    @Index(name = "idx_codigos_verif_usado", columnList = "usado")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CodigoVerificacionPago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    @Column(name = "numero_cuenta", nullable = false, columnDefinition = "TEXT")
    private String numeroCuenta;

    @Column(name = "curp", columnDefinition = "TEXT")
    private String curp;

    /**
     * Hash seguro (BCrypt) del código numérico de 6 dígitos. Nunca se almacena en claro.
     */
    @Column(name = "hash_codigo", nullable = false, columnDefinition = "TEXT")
    private String hashCodigo;

    /**
     * Hash SHA-256 de los datos de la transacción: (numeroCuenta + idProducto + idServicio + referencia/telefono + monto).
     * El código solo es válido para esta operación exacta.
     */
    @Column(name = "hash_operacion", nullable = false, columnDefinition = "TEXT")
    private String hashOperacion;

    @Column(name = "expira_en", nullable = false)
    private LocalDateTime expiraEn;

    @Column(name = "intentos", nullable = false)
    @Builder.Default
    private Integer intentos = 0;

    @Column(name = "usado", nullable = false)
    @Builder.Default
    private Boolean usado = false;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private LocalDateTime creadoEn;

    @PrePersist
    protected void onCreate() {
        if (this.creadoEn == null) {
            this.creadoEn = LocalDateTime.now();
        }
        if (this.intentos == null) {
            this.intentos = 0;
        }
        if (this.usado == null) {
            this.usado = false;
        }
    }
}
