package com.kapm.onboarding_clientes.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Cupo diario de llamadas a operaciones limitadas del proveedor (p. ej. getProductList.do: 3 por día).
 * Persistido en BD para que sobreviva a reinicios y se comparta entre instancias de la aplicación.
 */
@Entity
@Table(name = "consultas_proveedor_diarias", uniqueConstraints = {
    @UniqueConstraint(name = "uk_consulta_proveedor_operacion_fecha", columnNames = {"operacion", "fecha"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConsultaProveedorDiaria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "operacion", nullable = false, columnDefinition = "TEXT")
    private String operacion;

    /** Día en la zona horaria del proveedor. */
    @Column(name = "fecha", nullable = false)
    private LocalDate fecha;

    /** Llamadas realizadas (exitosas o no): el proveedor cuenta peticiones, no éxitos. */
    @Column(name = "consultas", nullable = false)
    private Integer consultas;

    @Column(name = "ultima_exitosa")
    private LocalDateTime ultimaExitosa;
}
