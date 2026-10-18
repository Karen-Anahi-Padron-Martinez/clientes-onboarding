package com.kapm.onboarding_clientes.dto.request;

import com.kapm.onboarding_clientes.validation.MontoDecimal;
import com.kapm.onboarding_clientes.validation.ValidationMessages;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Solicitud de registro de nuevo cliente persona física (100% Request Body, sin ID de BD)")
public class ClienteRegistrationRequest {

    // Datos Personales
    @NotBlank(message = ValidationMessages.CLIENTE_NOMBRE_OBLIGATORIO)
    @Size(min = 2, max = 50, message = ValidationMessages.CLIENTE_NOMBRE_SIZE)
    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]+$", message = ValidationMessages.CLIENTE_NOMBRE_PATTERN)
    @Schema(description = "Primer nombre del cliente", example = "Carlos")
    private String nombre;

    @Size(max = 50, message = ValidationMessages.CLIENTE_SEGUNDO_NOMBRE_SIZE)
    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]*$", message = ValidationMessages.CLIENTE_SEGUNDO_NOMBRE_PATTERN)
    @Schema(description = "Segundo nombre (opcional)", example = "Alberto")
    private String segundoNombre;

    @NotBlank(message = ValidationMessages.CLIENTE_APELLIDO_PATERNO_OBLIGATORIO)
    @Size(min = 2, max = 50, message = ValidationMessages.CLIENTE_APELLIDO_PATERNO_SIZE)
    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]+$", message = ValidationMessages.CLIENTE_APELLIDO_PATERNO_PATTERN)
    @Schema(description = "Apellido paterno", example = "Mendoza")
    private String apellidoPaterno;

    @NotBlank(message = ValidationMessages.CLIENTE_APELLIDO_MATERNO_OBLIGATORIO)
    @Size(min = 2, max = 50, message = ValidationMessages.CLIENTE_APELLIDO_MATERNO_SIZE)
    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]+$", message = ValidationMessages.CLIENTE_APELLIDO_MATERNO_PATTERN)
    @Schema(description = "Apellido materno", example = "Hernández")
    private String apellidoMaterno;

    @NotNull(message = ValidationMessages.CLIENTE_FECHA_NACIMIENTO_OBLIGATORIO)
    @Past(message = ValidationMessages.CLIENTE_FECHA_NACIMIENTO_PAST)
    @Schema(description = "Fecha de nacimiento (debe ser mayor de edad, YYYY-MM-DD)", example = "1992-08-24")
    private LocalDate fechaNacimiento;

    @NotBlank(message = ValidationMessages.CLIENTE_CURP_OBLIGATORIO)
    @Pattern(regexp = "^[A-Z]{4}\\d{6}[HM][A-Z]{5}[A-Z0-9]{2}$", message = ValidationMessages.CLIENTE_CURP_PATTERN)
    @Schema(description = "CURP oficial RENAPO (18 caracteres)", example = "MEHC920824HDFRMN99")
    private String curp;

    @NotBlank(message = ValidationMessages.CLIENTE_RFC_OBLIGATORIO)
    @Pattern(regexp = "^[A-Z&Ñ]{3,4}\\d{6}[A-Z0-9]{3}$", message = ValidationMessages.CLIENTE_RFC_PATTERN)
    @Schema(description = "RFC oficial SAT (12 o 13 caracteres)", example = "MEHC9208249A9")
    private String rfc;

    @NotBlank(message = ValidationMessages.CLIENTE_SEXO_OBLIGATORIO)
    @Schema(description = "Sexo (MASCULINO, FEMENINO, OTRO)", example = "MASCULINO")
    private String sexo;

    @NotBlank(message = ValidationMessages.CLIENTE_NACIONALIDAD_OBLIGATORIO)
    @Schema(description = "Nacionalidad estandarizada", example = "Mexicana")
    private String nacionalidad;

    @NotBlank(message = ValidationMessages.CLIENTE_ESTADO_CIVIL_OBLIGATORIO)
    @Schema(description = "Estado civil (SOLTERO, CASADO, DIVORCIADO, etc.)", example = "SOLTERO")
    private String estadoCivil;

    // Datos de Contacto
    @NotBlank(message = ValidationMessages.CLIENTE_CORREO_OBLIGATORIO)
    @Email(message = ValidationMessages.CLIENTE_CORREO_EMAIL)
    @Size(max = 100, message = ValidationMessages.CLIENTE_CORREO_SIZE)
    @Schema(description = "Correo electrónico único", example = "carlos.nuevo@email.com")
    private String correo;

    @NotBlank(message = ValidationMessages.CLIENTE_TELEFONO_MOVIL_OBLIGATORIO)
    @Pattern(regexp = "^\\d{10}$", message = ValidationMessages.CLIENTE_TELEFONO_MOVIL_PATTERN)
    @Schema(description = "Teléfono celular a 10 dígitos", example = "5598765432")
    private String telefonoMovil;

    @Pattern(regexp = "^(\\d{10})?$", message = ValidationMessages.CLIENTE_TELEFONO_ALT_PATTERN)
    @Schema(description = "Teléfono alternativo opcional a 10 dígitos", example = "5512344321")
    private String telefonoAlternativo;

    // Secciones complejas
    @NotNull(message = ValidationMessages.CLIENTE_DOMICILIO_OBLIGATORIO)
    @Valid
    private DomicilioDTO domicilio;

    @NotNull(message = ValidationMessages.CLIENTE_LABORAL_OBLIGATORIO)
    @Valid
    private DatosLaboralesDTO laboral;

    @Valid
    private DatosSeguridadDTO seguridad;

    @DecimalMin(value = "0.00", message = ValidationMessages.CLIENTE_SALDO_MIN)
    @MontoDecimal(message = ValidationMessages.CLIENTE_SALDO_MONTO)
    @Schema(description = "Saldo inicial de apertura para la cuenta bancaria (con 2 decimales)", example = "3500.00")
    private BigDecimal saldoInicial;
}
