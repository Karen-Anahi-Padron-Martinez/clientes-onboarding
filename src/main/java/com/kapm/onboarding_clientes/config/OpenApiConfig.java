package com.kapm.onboarding_clientes.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .servers(List.of(
                        new Server().url("http://localhost:8081").description("Servidor Local de Desarrollo")
                ))
                .info(new Info()
                        .title("Sistema de Onboarding de Clientes | Banca Digital API")
                        .version("1.1.0")
                        .description("### Documentación Integral OpenAPI / Swagger UI\n\n"
                                + "* **Registro Seguro de Clientes**: Creación automática de cuenta bancaria y saldo inicial sin exponer identificadores internos de base de datos (`id`).\n"
                                + "* **Actualización por CURP y RFC**: Identificación y actualización flexible por `RequestParam` (`?curp=...` o `?rfc=...`) o directamente en el `Request Body` JSON.\n"
                                + "* **Pre-validación en Tiempo Real**: Endpoints de verificación sintáctica oficial (RENAPO / SAT) y comprobación de disponibilidad previa.\n"
                                + "* **Seguridad Financiera y Biometría**: Cifrado BCrypt y SHA-256 con temporizador de inactividad de 5 segundos (`loggeado: true/false`).\n"
                                + "* **Catálogos Estandarizados**: Normalización de sexos, nacionalidades, estados civiles, 32 estados de México y ocupaciones.")
                        .contact(new Contact()
                                .name("Equipo de Desarrollo Financiero")
                                .email("soporte@onboarding-bancario.com"))
                        .license(new License().name("Apache 2.0").url("https://springdoc.org")));
    }
}
