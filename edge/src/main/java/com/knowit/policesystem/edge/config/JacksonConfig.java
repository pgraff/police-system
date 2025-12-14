package com.knowit.policesystem.edge.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.knowit.policesystem.edge.util.FlexibleInstantDeserializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.time.Instant;

/**
 * Jackson configuration for custom deserializers.
 * Registers FlexibleInstantDeserializer to accept multiple date formats.
 */
@Configuration
public class JacksonConfig {

    @Bean
    @Primary
    public ObjectMapper objectMapper(Jackson2ObjectMapperBuilder builder) {
        ObjectMapper mapper = builder.build();
        
        SimpleModule module = new SimpleModule();
        module.addDeserializer(Instant.class, new FlexibleInstantDeserializer());
        mapper.registerModule(module);
        
        return mapper;
    }
}
