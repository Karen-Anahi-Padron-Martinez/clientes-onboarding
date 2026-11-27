package com.kapm.onboarding_clientes.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Códigos de un solo uso enviados al correo para el segundo factor del login y la recuperación de contraseña.
 * Igual que en los códigos de pago, el código solo se guarda como hash BCrypt.
 */
@Entity
@Table(name = "codigos_acceso", indexes = {
    @Index(name = "idx_codigos_acceso_username", columnList = "username"),
    @Index(name = "idx_codigos_acceso_creado", columnList = "creado_en")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CodigoAcceso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String username;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "TEXT")
    private PropositoCodigoAcceso proposito;

    @Column(name = "hash_codigo", nullable = false, columnDefinition = "TEXT")
    private String hashCodigo;

    @Column(name = "expira_en", nullable = false)
    private LocalDateTime expiraEn;

    @Column(nullable = false)
    @Builder.Default
    private Integer intentos = 0;

    @Column(nullable = false)
    @Builder.Default
    private Boolean usado = false;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private LocalDateTime creadoEn;

    @PrePersist
    protected void onCreate() {
        if (this.creadoEn == null) {
            this.creadoEn = LocalDateTime.now();
        }
    }
}
