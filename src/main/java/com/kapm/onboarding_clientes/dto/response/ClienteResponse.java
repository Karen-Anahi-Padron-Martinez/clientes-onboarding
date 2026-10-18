package com.kapm.onboarding_clientes.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.kapm.onboarding_clientes.dto.request.DatosLaboralesDTO;
import com.kapm.onboarding_clientes.dto.request.DomicilioDTO;
import com.kapm.onboarding_clientes.entity.TipoBiometria;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Respuesta integral con datos del cliente (100% sin exponer IDs internos de BD ni datos biométricos sensibles)")
public class ClienteResponse {

    @JsonIgnore
    @Schema(hidden = true)
    private Long id;
    
    // Datos Personales
    @Schema(description = "Nombre(s)", example = "Carlos")
    private String nombre;

    @Schema(description = "Segundo nombre", example = "Alberto")
    private String segundoNombre;

    @Schema(description = "Apellido paterno", example = "Mendoza")
    private String apellidoPaterno;

    @Schema(description = "Apellido materno", example = "Hernández")
    private String apellidoMaterno;

    @Schema(description = "Nombre completo concatenado", example = "Carlos Alberto Mendoza Hernández")
    private String nombreCompleto;

    @Schema(description = "Fecha de nacimiento", example = "1992-08-24")
    private LocalDate fechaNacimiento;

    @Schema(description = "Edad calculada en años cumplidos", example = "33")
    private Integer edad;

    @Schema(description = "Clave oficial CURP (18 caracteres)", example = "MEHC920824HDFRMN09")
    private String curp;

    @Schema(description = "Clave oficial RFC (12 o 13 caracteres)", example = "MEHC9208243A8")
    private String rfc;

    @Schema(description = "Sexo", example = "MASCULINO")
    private String sexo;

    @Schema(description = "Nacionalidad", example = "Mexicana")
    private String nacionalidad;

    @Schema(description = "Estado civil", example = "SOLTERO")
    private String estadoCivil;

    // Datos de Contacto
    @Schema(description = "Correo electrónico", example = "carlos.mendoza@email.com")
    private String correo;

    @Schema(description = "Teléfono móvil", example = "5598765432")
    private String telefonoMovil;

    @Schema(description = "Teléfono alternativo", example = "5512344321")
    private String telefonoAlternativo;

    // Secciones complejas
    private DomicilioDTO domicilio;
    private DatosLaboralesDTO laboral;
    private CuentaResponse cuenta;

    // Datos de Seguridad y Sesión
    @Schema(description = "Username de acceso", example = "carlos_mendoza")
    private String username;

    @Schema(description = "Bandera de sesión activa", example = "false")
    private Boolean loggeado;

    @Schema(description = "Tipo de biometría registrada", example = "HUELLA_DACTILAR")
    private TipoBiometria tipoBiometriaRegistrada;

    @Schema(description = "Indica si el cliente cuenta con biometría registrada de manera segura", example = "true")
    private Boolean biometriaRegistrada;

    @Schema(description = "Segundos transcurridos desde última actividad", example = "0")
    private Long segundosUltimaActividad;

    // Estatus
    @Schema(description = "Estatus activo del cliente", example = "true")
    private Boolean activo;

    @Schema(description = "Fecha y hora de registro en el sistema")
    private LocalDateTime fechaRegistro;
}
