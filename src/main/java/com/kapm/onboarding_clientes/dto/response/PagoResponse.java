package com.kapm.onboarding_clientes.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.kapm.onboarding_clientes.config.JacksonConfig;
import com.kapm.onboarding_clientes.entity.EstatusPago;
import com.kapm.onboarding_clientes.entity.PagoServicio;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO de respuesta para operaciones de pago de servicios y recargas TAE.
 * Proporciona el estatus, números de autorización y códigos de respuesta de PuntoRed.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Respuesta detallada de la operación de pago/recarga")
public class PagoResponse {

    @Schema(description = "Código único de transacción interna UPC (máx. 15 caracteres)", example = "LS32K918A4F7Z")
    private String upc;

    @Schema(description = "Llave de idempotencia única asociada a la petición", example = "TX-8921-AF3")
    private String idempotencyKey;

    @Schema(description = "Número único de cuenta bancaria", example = "4819204812")
    private String numeroCuenta;

    @Schema(description = "Identificador del producto en PuntoRed", example = "55")
    private Integer idProducto;

    @Schema(description = "Identificador del servicio en PuntoRed", example = "2")
    private Integer idServicio;

    @Schema(description = "Número telefónico asociado a la operación", example = "5512345678")
    private String telefono;

    @Schema(description = "Referencia del recibo o factura", example = "REC-CFE-001")
    private String referencia;

    @JsonSerialize(using = JacksonConfig.BigDecimalTwoDecimalsSerializer.class)
    @Schema(description = "Monto de la transacción", example = "150.00")
    private BigDecimal monto;

    @JsonSerialize(using = JacksonConfig.BigDecimalTwoDecimalsSerializer.class)
    @Schema(description = "Comisión de servicio cobrada por el proveedor", example = "12.00")
    private BigDecimal comision;

    @Schema(description = "Estatus actual o final del pago (EXITOSO, RECHAZADO, PENDIENTE)", example = "EXITOSO")
    private EstatusPago estatus;

    @Schema(description = "Identificador de transacción asignado por PuntoRed (ID_TX)", example = "84920194")
    private String idTxProveedor;

    @Schema(description = "Número de autorización emitido por PuntoRed / Carrier (NUM_AUTORIZACION)", example = "AUT-99210")
    private String numAutorizacion;

    @Schema(description = "Código de respuesta emitido por PuntoRed", example = "01")
    private String codigoRespuesta;

    @Schema(description = "Texto descriptivo de la respuesta de PuntoRed", example = "TRANSACCION APROBADA")
    private String textoRespuesta;

    @Schema(description = "Mensaje informativo para el usuario", example = "Pago procesado exitosamente")
    private String mensaje;

    @Schema(description = "Fecha y hora de creación de la transacción")
    private LocalDateTime fechaCreacion;

    @Schema(description = "Fecha y hora de última actualización de la transacción")
    private LocalDateTime fechaActualizacion;

    public static PagoResponse desde(PagoServicio pago, String mensaje) {
        return PagoResponse.builder()
                .upc(pago.getUpc())
                .idempotencyKey(pago.getIdempotencyKey())
                .numeroCuenta(pago.getNumeroCuenta())
                .idProducto(pago.getIdProducto())
                .idServicio(pago.getIdServicio())
                .telefono(pago.getTelefono())
                .referencia(pago.getReferencia())
                .monto(pago.getMonto())
                .comision(pago.getComision())
                .estatus(pago.getEstatus())
                .idTxProveedor(pago.getIdTxProveedor())
                .numAutorizacion(pago.getNumAutorizacion())
                .codigoRespuesta(pago.getCodigoRespuesta())
                .textoRespuesta(pago.getTextoRespuesta())
                .mensaje(mensaje)
                .fechaCreacion(pago.getFechaCreacion())
                .fechaActualizacion(pago.getFechaActualizacion())
                .build();
    }
}
