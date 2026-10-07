package com.kapm.onboarding_clientes.model;

import com.kapm.onboarding_clientes.model.catalogo.*;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "clientes", indexes = {
    @Index(name = "idx_clientes_curp", columnList = "curp", unique = true),
    @Index(name = "idx_clientes_rfc", columnList = "rfc", unique = true),
    @Index(name = "idx_clientes_correo", columnList = "correo", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Datos Personales
    @Column(nullable = false, columnDefinition = "TEXT")
    private String nombre;

    @Column(name = "segundo_nombre", columnDefinition = "TEXT")
    private String segundoNombre;

    @Column(name = "apellido_paterno", nullable = false, columnDefinition = "TEXT")
    private String apellidoPaterno;

    @Column(name = "apellido_materno", nullable = false, columnDefinition = "TEXT")
    private String apellidoMaterno;

    @Column(name = "fecha_nacimiento", nullable = false)
    private LocalDate fechaNacimiento;

    @Column(nullable = false, unique = true, columnDefinition = "TEXT")
    private String curp;

    @Column(nullable = false, unique = true, columnDefinition = "TEXT")
    private String rfc;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "sexo_id", referencedColumnName = "id", nullable = false)
    private CatSexo sexo;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "nacionalidad_id", referencedColumnName = "id", nullable = false)
    private CatNacionalidad nacionalidad;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "estado_civil_id", referencedColumnName = "id", nullable = false)
    private CatEstadoCivil estadoCivil;

    // Datos de Contacto
    @Column(nullable = false, unique = true, columnDefinition = "TEXT")
    private String correo;

    @Column(name = "telefono_movil", nullable = false, columnDefinition = "TEXT")
    private String telefonoMovil;

    @Column(name = "telefono_alternativo", columnDefinition = "TEXT")
    private String telefonoAlternativo;

    // Información Laboral
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "ocupacion_id", referencedColumnName = "id", nullable = false)
    private CatOcupacion ocupacion;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String empresa;

    @Column(name = "ingreso_mensual", nullable = false, precision = 15, scale = 2)
    private BigDecimal ingresoMensual;

    // Estatus y Control
    @Column(nullable = false)
    private Boolean activo;

    @Column(name = "fecha_registro", nullable = false, updatable = false)
    private LocalDateTime fechaRegistro;

    // Relaciones
    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "domicilio_id", referencedColumnName = "id", nullable = false)
    private Domicilio domicilio;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "cuenta_id", referencedColumnName = "id", nullable = false)
    private Cuenta cuenta;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "seguridad_id", referencedColumnName = "id")
    private DatosSeguridadBiometria datosSeguridad;

    @PrePersist
    protected void onCreate() {
        this.fechaRegistro = LocalDateTime.now();
        if (this.activo == null) {
            this.activo = true;
        }
    }
}
