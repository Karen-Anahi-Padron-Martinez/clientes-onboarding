package com.kapm.onboarding_clientes.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "brevo")
public class BrevoProperties {
    private String apiKey;
    private String senderEmail;
    private String senderName = "Banco Digital";
    private String baseUrl = "https://api.brevo.com";
    private int connectTimeout = 5000;
    private int readTimeout = 15000;
}
