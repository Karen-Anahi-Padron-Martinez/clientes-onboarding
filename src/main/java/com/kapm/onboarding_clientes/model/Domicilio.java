package com.kapm.onboarding_clientes.model;

import com.kapm.onboarding_clientes.model.catalogo.CatEstadoRepublica;
import com.kapm.onboarding_clientes.model.catalogo.CatPais;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "domicilios")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Domicilio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String calle;

    @Column(name = "numero_exterior", nullable = false, columnDefinition = "TEXT")
    private String numeroExterior;

    @Column(name = "numero_interior", columnDefinition = "TEXT")
    private String numeroInterior;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String colonia;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String municipio;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "estado_id", referencedColumnName = "id", nullable = false)
    private CatEstadoRepublica estado;

    @Column(name = "codigo_postal", nullable = false, columnDefinition = "TEXT")
    private String codigoPostal;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "pais_id", referencedColumnName = "id", nullable = false)
    private CatPais pais;
}
