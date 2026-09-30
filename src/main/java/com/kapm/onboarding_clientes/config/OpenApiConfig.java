package com.kapm.onboarding_clientes.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Sistema de Onboarding de Clientes / Institución Financiera API")
                        .version("1.0.0")
                        .description("API RESTful para el registro de clientes personas físicas, validaciones de negocio en tiempo real, creación automática de cuentas bancarias, encriptación BCrypt/SHA-256 de login y datos biométricos.")
                        .contact(new Contact()
                                .name("Equipo de Desarrollo Financiero")
                                .email("soporte@onboarding-bancario.com"))
                        .license(new License().name("Apache 2.0").url("https://springdoc.org")));
    }
}
