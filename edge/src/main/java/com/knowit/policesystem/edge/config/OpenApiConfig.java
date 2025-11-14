package com.knowit.policesystem.edge.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI/Swagger configuration.
 * Configures API documentation based on the existing OpenAPI specification.
 */
@Configuration
public class OpenApiConfig {

    /**
     * Configures the OpenAPI information.
     * Matches the information from the existing OpenAPI specification.
     *
     * @return OpenAPI configuration
     */
    @Bean
    public OpenAPI policeSystemOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Police Incident Management System API")
                        .description("""
                                REST API for the Police Incident Management System. This is an event-driven system where
                                all operations produce events to Kafka. Events represent requests/commands from the edge,
                                not state changes.
                                
                                ## Architecture
                                - Edge servers receive HTTP requests and produce events to Kafka
                                - Events represent requests/commands, not state changes
                                - No state reconstruction in edge layer
                                - All operations are asynchronous via Kafka events
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Police System API Support"))
                        .license(new License()
                                .name("Proprietary")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080/api")
                                .description("Local development server"),
                        new Server()
                                .url("https://api.policesystem.example.com/api")
                                .description("Production server")));
    }

    /**
     * Configures grouped OpenAPI for API documentation.
     * Groups all endpoints under the main API group.
     *
     * @return GroupedOpenApi configuration
     */
    @Bean
    public GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder()
                .group("police-system-api")
                .pathsToMatch("/api/v1/**")
                .build();
    }
}

