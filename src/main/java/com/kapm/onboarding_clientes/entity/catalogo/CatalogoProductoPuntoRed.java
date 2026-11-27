package com.kapm.onboarding_clientes.entity.catalogo;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Entidad para almacenar el catálogo local de productos y servicios de PuntoRed.
 * Permite resolver el idProducto, idServicio y tipoFront sin depender de la red en cada transacción.
 */
@Entity
@Table(name = "catalogo_productos_puntored", indexes = {
    @Index(name = "idx_cat_prod_pr_id_prod", columnList = "id_producto", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CatalogoProductoPuntoRed {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "id_producto", nullable = false, unique = true)
    private Integer idProducto;

    @Column(name = "id_servicio", nullable = false)
    private Integer idServicio;

    @Column(name = "nombre_producto", nullable = false, columnDefinition = "TEXT")
    private String nombreProducto;

    @Column(name = "nombre_servicio", nullable = false, columnDefinition = "TEXT")
    private String nombreServicio;

    @Column(name = "id_cat_tipo_servicio")
    private Integer idCatTipoServicio;

    @Column(name = "tipo_front", nullable = false)
    private Integer tipoFront;

    @Column(name = "activo", nullable = false)
    @Builder.Default
    private Boolean activo = true;

    /**
     * Restricciones de monto definidas por el negocio (PuntoRed no las expone en getProductList).
     * Si son nulas se aplican los límites globales de {@code puntored.monto-minimo-operacion} / {@code monto-maximo-operacion}.
     * Para productos de monto fijo (p. ej. paquetes TAE) se configura montoMinimo == montoMaximo.
     */
    @Column(name = "monto_minimo", precision = 15, scale = 2)
    private BigDecimal montoMinimo;

    @Column(name = "monto_maximo", precision = 15, scale = 2)
    private BigDecimal montoMaximo;

    /*
     * Datos de presentación administrados localmente: getProductList no los entrega y la sincronización
     * no los sobrescribe. Todos son opcionales.
     */
    @Column(name = "logo_url", columnDefinition = "TEXT")
    private String logoUrl;

    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "categoria", columnDefinition = "TEXT")
    private String categoria;

    /**
     * Tipo de referencia de Gestopago: "a" = número telefónico a 10 dígitos (recargas, paquetes y tarjetas
     * que se entregan al celular); "b"/"c" = referencia del recibo.
     */
    @Column(name = "tipo_referencia", columnDefinition = "TEXT")
    private String tipoReferencia;

    /** Expresión regular que debe cumplir la referencia (servicios) o el teléfono (recargas). */
    @Column(name = "regex_referencia", columnDefinition = "TEXT")
    private String regexReferencia;
}
