package com.kapm.onboarding_clientes.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")
                                .description("Token devuelto por POST /api/auth/login (clientes)"))
                        .addSecuritySchemes("basicAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP).scheme("basic")
                                .description("Usuario ADMIN definido por ADMIN_USER / ADMIN_PASSWORD")))
                // URL relativa: Swagger UI llama al mismo origen desde el que se abrió (localhost o Render, con su HTTPS)
                .servers(List.of(
                        new Server().url("/").description("Servidor actual")
                ))
                .info(new Info()
                        .title("Sistema de Onboarding de Clientes | Banca Digital API")
                        .version("2.0.0")
                        .description("### Documentación Integral OpenAPI / Swagger UI (100% Request Body - Cero IDs de BD)\n\n"
                                + "* **Búsqueda Dinámica Multicriterio**: Endpoint unificado `POST /api/clientes/buscar` mediante Request Body con filtros combinables.\n"
                                + "* **Actualización por CURP y RFC**: Endpoints dedicados `PUT /api/clientes/curp` y `PUT /api/clientes/rfc` identificando al cliente sin ID.\n"
                                + "* **Cero Uso de IDs de Base de Datos**: Todas las operaciones de consulta, modificación, baja y reactivación se realizan exclusivamente con CURP y RFC.\n"
                                + "* **Registro Seguro de Clientes**: Creación automática de cuenta bancaria y saldo inicial sin exponer identificadores internos de base de datos (`id`).\n"
                                + "* **Pre-validación en Tiempo Real**: Verificación sintáctica oficial (RENAPO / SAT) y comprobación de disponibilidad previa (`POST /api/clientes/validar`).\n"
                                + "* **Seguridad Financiera y Biometría**: Cifrado BCrypt y SHA-256 con temporizador de inactividad de 5 segundos (`loggeado: true/false`).\n"
                                + "* **Catálogos Estandarizados**: Normalización de sexos, nacionalidades, estados civiles, 32 estados de México y ocupaciones.")
                        .contact(new Contact()
                                .name("Equipo de Desarrollo Financiero")
                                .email("soporte@onboarding-bancario.com"))
                        .license(new License().name("Apache 2.0").url("https://springdoc.org")));
    }
}
