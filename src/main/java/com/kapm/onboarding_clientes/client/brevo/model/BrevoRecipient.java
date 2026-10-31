package com.kapm.onboarding_clientes.client.brevo.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

/**
 * Modelo para un destinatario en la API de Brevo.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BrevoRecipient {
    private String email;
    private String name;
}
