package com.fleet.fleet.config;

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
    public OpenAPI fleetServiceOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Fleet Manager Fleet Service API")
                        .version("1.0.0")
                        .description("Vehicle registry APIs for Fleet Manager.")
                        .contact(new Contact().name("Fleet Manager Team")))
                .servers(List.of(new Server()
                        .url("/api/fleet")
                        .description("Fleet service through Traefik")))
                .components(new Components().addSecuritySchemes("bearerAuth",
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
