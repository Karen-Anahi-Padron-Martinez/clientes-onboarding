package com.kapm.onboarding_clientes.model.catalogo;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "cat_estados_civiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CatEstadoCivil {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, columnDefinition = "TEXT")
    private String codigo;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String descripcion;
}
