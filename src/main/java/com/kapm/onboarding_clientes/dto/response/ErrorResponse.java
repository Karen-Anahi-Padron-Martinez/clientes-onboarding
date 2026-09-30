package com.kapm.onboarding_clientes.dto.response;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ErrorResponse {

    private boolean exito;
    private int status;
    private String error;
    private String mensaje;
    private String ruta;
    private Map<String, String> erroresValidacion;
    private LocalDateTime timestamp;
}
