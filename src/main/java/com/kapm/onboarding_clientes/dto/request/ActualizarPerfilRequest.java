package com.kapm.onboarding_clientes.dto.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.kapm.onboarding_clientes.config.JacksonConfig;
import com.kapm.onboarding_clientes.validation.MontoDecimal;
import com.kapm.onboarding_clientes.validation.PatronesTexto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Datos que el cliente puede modificar desde su perfil. CURP y RFC no forman parte del contrato:
 * como Jackson rechaza propiedades desconocidas, enviarlos responde 400.
 * Si cambia el correo (canal del segundo factor) se exige la contraseña actual.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "passwordActual")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Actualización del perfil del cliente autenticado (CURP y RFC no se pueden modificar)")
public class ActualizarPerfilRequest {

    @NotBlank(message = "El nombre es obligatorio")
    @Size(min = 3, max = 50, message = "El nombre debe tener entre 3 y 50 caracteres")
    @Pattern(regexp = PatronesTexto.NOMBRE, message = "El nombre solo admite letras y espacios")
    private String nombre;

    @Size(max = 50, message = "El segundo nombre no puede exceder 50 caracteres")
    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚüÜñÑ\\s]*$", message = "El segundo nombre solo admite letras y espacios")
    private String segundoNombre;

    @NotBlank(message = "El apellido paterno es obligatorio")
    @Size(min = 3, max = 50, message = "El apellido paterno debe tener entre 3 y 50 caracteres")
    @Pattern(regexp = PatronesTexto.NOMBRE, message = "El apellido paterno solo admite letras y espacios")
    private String apellidoPaterno;

    @NotBlank(message = "El apellido materno es obligatorio")
    @Size(min = 3, max = 50, message = "El apellido materno debe tener entre 3 y 50 caracteres")
    @Pattern(regexp = PatronesTexto.NOMBRE, message = "El apellido materno solo admite letras y espacios")
    private String apellidoMaterno;

    @NotNull(message = "La fecha de nacimiento es obligatoria")
    @Past(message = "La fecha de nacimiento debe ser una fecha pasada")
    private LocalDate fechaNacimiento;

    @NotBlank(message = "El sexo es obligatorio")
    private String sexo;

    @NotBlank(message = "La nacionalidad es obligatoria")
    private String nacionalidad;

    @NotBlank(message = "El estado civil es obligatorio")
    private String estadoCivil;

    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "El correo no tiene un formato válido")
    @Size(max = 100, message = "El correo no puede exceder 100 caracteres")
    private String correo;

    @NotBlank(message = "El teléfono móvil es obligatorio")
    @Pattern(regexp = "^\\d{10}$", message = "El teléfono móvil debe tener 10 dígitos")
    private String telefonoMovil;

    @Pattern(regexp = "^(\\d{10})?$", message = "El teléfono alternativo debe tener 10 dígitos")
    private String telefonoAlternativo;

    @NotNull(message = "El domicilio es obligatorio")
    @Valid
    private DomicilioDTO domicilio;

    @NotBlank(message = "La ocupación es obligatoria")
    private String ocupacion;

    @Size(max = 100, message = "La empresa no puede exceder 100 caracteres")
    @Pattern(regexp = PatronesTexto.EMPRESA, message = PatronesTexto.MENSAJE_EMPRESA)
    private String empresa;

    @NotNull(message = "El ingreso mensual es obligatorio")
    @DecimalMin(value = "0.01", message = "El ingreso mensual debe ser mayor a 0")
    @DecimalMax(value = "9999999.99", message = "El ingreso mensual no puede exceder 9,999,999.99")
    @MontoDecimal(message = "El ingreso mensual debe tener exactamente 2 decimales")
    @JsonSerialize(using = JacksonConfig.BigDecimalTwoDecimalsSerializer.class)
    private BigDecimal ingresoMensual;

    @Schema(description = "Obligatoria solo si cambia el correo")
    private String passwordActual;
}
