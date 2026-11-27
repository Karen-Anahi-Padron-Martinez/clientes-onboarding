package com.kapm.onboarding_clientes.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.kapm.onboarding_clientes.dto.request.ActualizarPerfilRequest;
import com.kapm.onboarding_clientes.dto.request.DomicilioDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Datos del titular del token para la sección "Perfil" de las apps. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Perfil del cliente autenticado")
public class PerfilResponse {

    private String username;
    private String nombreCompleto;
    private LocalDate fechaNacimiento;
    private String curp;
    private String rfc;
    private String correo;
    private String telefonoMovil;
    private String telefonoAlternativo;
    private String ocupacion;
    private String empresa;
    private DomicilioDTO domicilio;
    private String numeroCuenta;
    private String estatusCuenta;
    private LocalDateTime fechaRegistro;

    /** Valores actuales con los códigos de catálogo, listos para precargar el formulario de edición. */
    private ActualizarPerfilRequest datosEditables;
}
