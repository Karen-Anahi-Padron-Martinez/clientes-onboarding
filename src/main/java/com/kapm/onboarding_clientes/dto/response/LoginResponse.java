package com.kapm.onboarding_clientes.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.kapm.onboarding_clientes.entity.TipoBiometria;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LoginResponse {

    private boolean autenticado;
    private boolean loggeado;
    private String mensaje;
    private String username;
    private Long clienteId;
    private String clienteNombreCompleto;
    private String numeroCuenta;
    /** Identificadores del titular: los pagos los exigen en el body y deben coincidir con el dueño del token. */
    private String curp;
    private String rfc;
    private TipoBiometria tipoBiometriaValidada;
    /** JWT de sesión (Bearer). Requerido por /api/pagos/**, /api/auth/estado y /api/auth/logout. */
    private String accessToken;
    private String tokenType;
    private java.time.Instant expiraEn;
    private Long segundosDesdeUltimaActividad;
    private String estadoSesion;
    private LocalDateTime timestamp;
}
