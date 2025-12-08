package com.knowit.policesystem.edge.config;

import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * OpenAPI/Swagger configuration.
 * Loads API documentation from the existing OpenAPI YAML specification file.
 */
@Configuration
public class OpenApiConfig {

    /**
     * Loads the OpenAPI specification from the YAML file in resources.
     * The YAML file is located at {@code api/openapi.yaml} in the classpath.
     *
     * @return OpenAPI configuration loaded from YAML file
     * @throws RuntimeException if the YAML file cannot be loaded or parsed
     */
    @Bean
    public io.swagger.v3.oas.models.OpenAPI policeSystemOpenAPI() {
        try {
            Resource resource = new ClassPathResource("api/openapi.yaml");
            if (!resource.exists()) {
                throw new IllegalStateException("OpenAPI YAML file not found at api/openapi.yaml");
            }

            try (InputStream inputStream = resource.getInputStream()) {
                String yamlContent = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);

                OpenAPIV3Parser parser = new OpenAPIV3Parser();
                ParseOptions options = new ParseOptions();
                options.setResolve(true);
                options.setFlatten(true);

                SwaggerParseResult parseResult = parser.readContents(yamlContent, null, options);

                if (parseResult.getOpenAPI() == null) {
                    String errorMessage = "Failed to parse OpenAPI YAML file. Errors: " +
                            String.join(", ", parseResult.getMessages());
                    throw new IllegalStateException(errorMessage);
                }

                return parseResult.getOpenAPI();
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load OpenAPI YAML file from classpath", e);
        }
    }

    /**
     * Configures grouped OpenAPI for API documentation.
     * Groups all endpoints under the main API group.
     * Note: The OpenAPI spec defines paths starting with /api, but controllers use /api/v1.
     * This grouping matches the actual controller paths.
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

