package com.fleet.parser.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI parserServiceOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Fleet Manager Parser Service API")
                        .version("1.0.0")
                        .description("Document parser service APIs for Fleet Manager.")
                        .contact(new Contact().name("Fleet Manager Team")))
                .servers(List.of(new Server()
                                .url("/api/parser")
                                .description("Parser service through Traefik"),
                        new Server()
                                .url("/")
                                .description("Direct parser-service local port")))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT"))
                        .addSecuritySchemes("internalApiKey",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.APIKEY)
                                        .in(SecurityScheme.In.HEADER)
                                        .name("X-Internal-Api-Key")));
    }
}
