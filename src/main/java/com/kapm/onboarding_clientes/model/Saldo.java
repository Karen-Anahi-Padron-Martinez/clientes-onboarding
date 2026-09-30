package com.kapm.onboarding_clientes.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "saldos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Saldo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "cuenta_id", referencedColumnName = "id", nullable = false, unique = true)
    private Cuenta cuenta;

    @Column(name = "saldo_disponible", nullable = false, precision = 15, scale = 2)
    private BigDecimal saldoDisponible;

    @Column(name = "saldo_contable", nullable = false, precision = 15, scale = 2)
    private BigDecimal saldoContable;

    @Column(name = "fecha_ultima_actualizacion", nullable = false)
    private LocalDateTime fechaUltimaActualizacion;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        this.fechaUltimaActualizacion = LocalDateTime.now();
    }
}
