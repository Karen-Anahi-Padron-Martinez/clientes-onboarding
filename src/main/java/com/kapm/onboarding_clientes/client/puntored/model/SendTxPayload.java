package com.kapm.onboarding_clientes.client.puntored.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.kapm.onboarding_clientes.exception.PuntoRedIntegracionException;
import lombok.*;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Payload en claro con los datos para ejecutar una transacción en PuntoRed (sendTx.do).
 * Contiene únicamente los campos oficiales requeridos.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"telefono", "referencia"})
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SendTxPayload {

    private int idProducto;
    private int idServicio;
    private String telefono;
    private String horaLocal;
    private String referencia;
    private BigDecimal montoPago;
    private String upc;
    private String unidad;

    /**
     * Campo contextual para identificar si la transacción requiere referencia y monto (Pagos de Servicios).
     * Ignorado en serialización JSON/XML para que NUNCA viaje en la trama oficial enviada a PuntoRed.
     */
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Integer tipoFront;

    /**
     * Valida la obligatoriedad y longitudes de todos los campos en claro antes del proceso de cifrado.
     *
     * @throws PuntoRedIntegracionException Si algún campo no cumple con las reglas oficiales
     */
    public void validar() {
        if (idProducto <= 0) {
            throw new PuntoRedIntegracionException("El campo 'idProducto' debe ser un número entero positivo mayor a cero");
        }
        if (idServicio <= 0) {
            throw new PuntoRedIntegracionException("El campo 'idServicio' debe ser un número entero positivo mayor a cero");
        }
        if (telefono == null || telefono.trim().length() < 10 || telefono.trim().length() > 20) {
            throw new PuntoRedIntegracionException("El campo 'telefono' es obligatorio y debe tener entre 10 y 20 caracteres");
        }
        if (horaLocal == null || !horaLocal.matches("^\\d{14}$")) {
            throw new PuntoRedIntegracionException("El campo 'horaLocal' es obligatorio y debe tener el formato numérico 'yyyyMMddHHmmss' (14 dígitos)");
        }
        if (upc == null || upc.trim().length() < 3 || upc.trim().length() > 15) {
            throw new PuntoRedIntegracionException("El campo 'upc' es obligatorio y debe tener entre 3 y 15 caracteres");
        }
        if (unidad == null || unidad.trim().length() < 3 || unidad.trim().length() > 25) {
            throw new PuntoRedIntegracionException("El campo 'unidad' es obligatorio y debe tener entre 3 y 25 caracteres");
        }

        // Validación condicional de servicios según tipoFront del producto
        boolean esRecarga = (tipoFront != null && tipoFront == 0);
        boolean esPagoServicio = (tipoFront != null && (tipoFront == 1 || tipoFront == 2))
                || (!esRecarga && referencia != null && !referencia.isBlank());

        if (esPagoServicio) {
            if (referencia == null || referencia.trim().isEmpty() || referencia.trim().length() > 100) {
                throw new PuntoRedIntegracionException("El campo 'referencia' es obligatorio para pagos de servicios y debe tener entre 1 y 100 caracteres");
            }
            if (montoPago == null || montoPago.compareTo(BigDecimal.ZERO) <= 0) {
                throw new PuntoRedIntegracionException("El campo 'montoPago' es obligatorio para pagos de servicios y debe ser mayor a cero");
            }
            if (montoPago.scale() > 2) {
                throw new PuntoRedIntegracionException("El campo 'montoPago' no puede tener más de 2 decimales");
            }
        } else if (esRecarga) {
            if (montoPago == null || montoPago.compareTo(BigDecimal.ZERO) <= 0) {
                throw new PuntoRedIntegracionException("El campo 'montoPago' es obligatorio para recargas y debe ser mayor a cero");
            }
            if (montoPago.scale() > 2) {
                throw new PuntoRedIntegracionException("El campo 'montoPago' no puede tener más de 2 decimales");
            }
        }
    }

    /**
     * Retorna el número de teléfono enmascarado para bitácoras y registros de auditoría (ej. ******1234).
     */
    public String getTelefonoEnmascarado() {
        if (telefono == null || telefono.length() < 4) {
            return "******";
        }
        String ultimosCuatro = telefono.substring(telefono.length() - 4);
        return "*".repeat(Math.max(6, telefono.length() - 4)) + ultimosCuatro;
    }
}
