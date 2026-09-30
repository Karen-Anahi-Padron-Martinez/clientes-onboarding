package com.kapm.onboarding_clientes.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "datos_seguridad_biometria", indexes = {
    @Index(name = "idx_seguridad_username", columnList = "username", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DatosSeguridadBiometria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, columnDefinition = "TEXT")
    private String username;

    @Column(name = "password_hash", nullable = false, columnDefinition = "TEXT")
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_biometria", nullable = false, columnDefinition = "TEXT")
    private TipoBiometria tipoBiometria;

    @Column(name = "hash_biometrico", nullable = false, columnDefinition = "TEXT")
    private String hashBiometrico;

    @Column(name = "loggeado", nullable = false)
    private Boolean loggeado;

    @Column(name = "ultima_actividad")
    private LocalDateTime ultimaActividad;

    @Column(name = "fecha_registro_biometrico", nullable = false, updatable = false)
    private LocalDateTime fechaRegistroBiometrico;

    @PrePersist
    protected void onCreate() {
        this.fechaRegistroBiometrico = LocalDateTime.now();
        if (this.loggeado == null) {
            this.loggeado = false;
        }
    }
}
