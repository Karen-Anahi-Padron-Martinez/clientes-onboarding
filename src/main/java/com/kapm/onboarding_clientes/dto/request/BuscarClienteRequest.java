package com.kapm.onboarding_clientes.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.kapm.onboarding_clientes.validation.ValidationMessages;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Criterios combinables para búsqueda dinámica de clientes mediante Request Body con paginación (sin uso de ID ni PathVariable)")
public class BuscarClienteRequest {

    @Pattern(regexp = "^$|^[a-zA-Z]{4}\\d{6}[hHmM][a-zA-Z]{5}[a-zA-Z0-9]{2}$", message = ValidationMessages.CLIENTE_CURP_PATTERN)
    @Schema(description = "Buscar cliente por su CURP oficial (18 caracteres)", example = "MEHC920824HDFRMN09")
    private String curp;

    @Pattern(regexp = "^$|^[a-zA-Z&ñÑ]{3,4}\\d{6}[a-zA-Z0-9]{3}$", message = ValidationMessages.CLIENTE_RFC_PATTERN)
    @Schema(description = "Buscar cliente por su RFC oficial (12 o 13 caracteres)", example = "MEHC9208243A8")
    private String rfc;

    @Email(message = ValidationMessages.CLIENTE_CORREO_EMAIL)
    @Schema(description = "Buscar cliente por Correo Electrónico", example = "carlos.mendoza@email.com")
    private String correo;

    @Pattern(regexp = "^$|^\\d{10}$", message = ValidationMessages.CONSULTA_CUENTA_PATTERN)
    @Schema(description = "Buscar cliente por Número de Cuenta Bancaria (10 dígitos)", example = "4819204812")
    private String numeroCuenta;

    @Schema(description = "Filtro por nombre (búsqueda parcial/insensible a mayúsculas)", example = "Carlos")
    private String nombre;

    @Schema(description = "Filtro por apellido paterno (búsqueda parcial/insensible a mayúsculas)", example = "Mendoza")
    private String apellidoPaterno;

    @Schema(description = "Filtro por apellido materno (búsqueda parcial/insensible a mayúsculas)", example = "Hernández")
    private String apellidoMaterno;

    @Schema(description = "Filtro por estatus de activación (true = activo, false = inactivo)", example = "true")
    private Boolean activo;

    @Schema(description = "Filtro de fecha de registro inicial (YYYY-MM-DD)", example = "2026-01-01")
    private LocalDate fechaInicio;

    @Schema(description = "Filtro de fecha de registro final (YYYY-MM-DD)", example = "2026-12-31")
    private LocalDate fechaFin;

    @Min(value = 0, message = ValidationMessages.BUSQUEDA_PAGINA_MIN)
    @Builder.Default
    @Schema(description = "Número de página para paginación (0-indexada)", example = "0", defaultValue = "0")
    private Integer pagina = 0;

    @Min(value = 1, message = ValidationMessages.BUSQUEDA_TAMANIO_MIN)
    @Max(value = 100, message = ValidationMessages.BUSQUEDA_TAMANIO_MAX)
    @Builder.Default
    @Schema(description = "Tamaño de la página (entre 1 y 100)", example = "10", defaultValue = "10")
    private Integer tamanio = 10;

    @JsonIgnore
    @AssertTrue(message = ValidationMessages.BUSQUEDA_CRITERIO_PRESENTE)
    public boolean isCriterioPresente() {
        return (curp != null && !curp.isBlank()) ||
               (rfc != null && !rfc.isBlank()) ||
               (correo != null && !correo.isBlank()) ||
               (numeroCuenta != null && !numeroCuenta.isBlank()) ||
               (nombre != null && !nombre.isBlank()) ||
               (apellidoPaterno != null && !apellidoPaterno.isBlank()) ||
               (apellidoMaterno != null && !apellidoMaterno.isBlank()) ||
               activo != null ||
               fechaInicio != null ||
               fechaFin != null;
    }

    public int getPaginaSegura() {
        return (pagina == null || pagina < 0) ? 0 : pagina;
    }

    public int getTamanioSeguro() {
        return (tamanio == null || tamanio < 1) ? 10 : tamanio;
    }
}
