package com.kapm.onboarding_clientes.client.brevo.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.util.List;

/**
 * Payload de solicitud para el envío de correos transaccionales en la API de Brevo (POST /v3/smtp/email).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"htmlContent"})
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BrevoEmailRequest {

    private BrevoSender sender;
    private List<BrevoRecipient> to;
    private String subject;
    private String htmlContent;
}
