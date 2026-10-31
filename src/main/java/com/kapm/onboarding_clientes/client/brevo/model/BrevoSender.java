package com.kapm.onboarding_clientes.client.brevo.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

/**
 * Modelo para el remitente del correo en la API de Brevo.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BrevoSender {
    private String name;
    private String email;
}
