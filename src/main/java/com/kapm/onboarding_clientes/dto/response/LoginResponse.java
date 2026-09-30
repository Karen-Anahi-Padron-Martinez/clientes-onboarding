package com.kapm.onboarding_clientes.dto.response;

import com.kapm.onboarding_clientes.model.TipoBiometria;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginResponse {

    private boolean autenticado;
    private boolean loggeado;
    private String mensaje;
    private String username;
    private Long clienteId;
    private String clienteNombreCompleto;
    private String numeroCuenta;
    private TipoBiometria tipoBiometriaValidada;
    private String tokenSimulado;
    private Long segundosDesdeUltimaActividad;
    private String estadoSesion;
    private LocalDateTime timestamp;
}
