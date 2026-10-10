package com.kapm.onboarding_clientes.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ValidacionIdentificadorResponse {

    private String identificador;
    private String tipo; // "CURP" o "RFC"
    private boolean formatoValido;
    private boolean disponible;
    private String mensaje;
}
