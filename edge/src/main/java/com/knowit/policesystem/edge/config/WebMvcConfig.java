package com.knowit.policesystem.edge.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC configuration for the edge module.
 * Configures API versioning, content negotiation, and path matching.
 */
@Configuration
@EnableScheduling
public class WebMvcConfig implements WebMvcConfigurer {

    /**
     * Configures content negotiation.
     * Defaults to JSON media type.
     *
     * @param configurer the content negotiation configurer
     */
    @Override
    public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
        configurer
                .defaultContentType(MediaType.APPLICATION_JSON)
                .mediaType("json", MediaType.APPLICATION_JSON);
    }

    /**
     * Configures path matching options.
     * Note: Trailing slash matching is enabled by default in Spring Boot 3.x.
     *
     * @param configurer the path match configurer
     */
    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        // Trailing slash matching is enabled by default in Spring Boot 3.x
        // No additional configuration needed
    }
}

