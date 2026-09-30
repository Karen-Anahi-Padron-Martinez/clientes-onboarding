package com.kapm.onboarding_clientes.dto.response;

import com.kapm.onboarding_clientes.dto.request.DatosLaboralesDTO;
import com.kapm.onboarding_clientes.dto.request.DomicilioDTO;
import com.kapm.onboarding_clientes.model.TipoBiometria;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClienteResponse {

    private Long id;
    
    // Datos Personales
    private String nombre;
    private String segundoNombre;
    private String apellidoPaterno;
    private String apellidoMaterno;
    private String nombreCompleto;
    private LocalDate fechaNacimiento;
    private Integer edad;
    private String curp;
    private String rfc;
    private String sexo;
    private String nacionalidad;
    private String estadoCivil;

    // Datos de Contacto
    private String correo;
    private String telefonoMovil;
    private String telefonoAlternativo;

    // Secciones complejas
    private DomicilioDTO domicilio;
    private DatosLaboralesDTO laboral;
    private CuentaResponse cuenta;

    // Datos de Seguridad y Sesión
    private String username;
    private Boolean loggeado;
    private TipoBiometria tipoBiometriaRegistrada;
    private String hashBiometricoResumido;
    private Long segundosUltimaActividad;

    // Estatus
    private Boolean activo;
    private LocalDateTime fechaRegistro;
}
