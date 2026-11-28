package com.kapm.onboarding_clientes.entity;

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
    @Column(name = "tipo_biometria", columnDefinition = "TEXT")
    private TipoBiometria tipoBiometria;

    @Column(name = "hash_biometrico", columnDefinition = "TEXT")
    private String hashBiometrico;

    @Column(name = "loggeado", nullable = false)
    private Boolean loggeado;

    /**
     * Identificador de la sesión vigente (claim 'jti' del JWT). Un nuevo login lo reemplaza y el logout lo borra,
     * con lo que cualquier token anterior deja de ser válido aunque no haya expirado.
     */
    @Column(name = "sesion_id", columnDefinition = "TEXT")
    private String sesionId;

    @Column(name = "ultima_actividad")
    private LocalDateTime ultimaActividad;

    @Column(name = "fecha_registro_biometrico", nullable = false, updatable = false)
    private LocalDateTime fechaRegistroBiometrico;

    /** Google Authenticator (TOTP RFC 6238): activo solo después de verificar el primer código. */
    @Column(name = "totp_habilitado")
    private Boolean totpHabilitado;

    @Column(name = "totp_secret", columnDefinition = "TEXT")
    private String totpSecret;

    @Column(name = "totp_secret_pendiente", columnDefinition = "TEXT")
    private String totpSecretPendiente;

    /** Último intervalo de 30 s consumido para evitar reutilización del mismo código TOTP. */
    @Column(name = "totp_ultimo_paso")
    private Long totpUltimoPaso;

    @PrePersist
    protected void onCreate() {
        this.fechaRegistroBiometrico = LocalDateTime.now();
        if (this.loggeado == null) {
            this.loggeado = false;
        }
        if (this.totpHabilitado == null) {
            this.totpHabilitado = false;
        }
    }
}
