package com.odyxs.vg.config;

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
                .info(new Info()
                        .title("Odyssey API — Guía Turística & Asistente IA")
                        .version("2.0.0-PROD")
                        .description("API RESTful para Cartagena de Indias. " +
                                "Arquitectura modular en capas con Java 21, Spring Boot 3, MySQL 8 y caché en memoria.")
                        .contact(new Contact()
                                .name("Equipo de Ingeniería Odyssey")
                                .email("soporte@odyssey.com")
                                .url("http://localhost:8080"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Servidor Local de Desarrollo")
                ));
    }
}
