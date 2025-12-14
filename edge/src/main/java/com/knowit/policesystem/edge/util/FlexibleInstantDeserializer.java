package com.knowit.policesystem.edge.util;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Flexible deserializer for Instant that accepts multiple ISO 8601 date formats.
 * Supports:
 * - yyyy-MM-dd'T'HH:mm:ssZ (e.g., 2024-01-15T10:40:00Z)
 * - yyyy-MM-dd'T'HH:mm:ssXXX (e.g., 2024-01-15T10:40:00+00:00)
 * - yyyy-MM-dd'T'HH:mm:ss.SSSXXX (e.g., 2024-01-15T10:40:00.000+00:00)
 * - yyyy-MM-dd'T'HH:mm:ss.SSSZ (e.g., 2024-01-15T10:40:00.000Z)
 */
public class FlexibleInstantDeserializer extends JsonDeserializer<Instant> {

    private static final DateTimeFormatter[] FORMATTERS = {
        // Standard ISO 8601 with Z
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").withZone(java.time.ZoneOffset.UTC),
        // ISO 8601 with timezone offset (XXX)
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX"),
        // ISO 8601 with milliseconds and Z
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(java.time.ZoneOffset.UTC),
        // ISO 8601 with milliseconds and timezone offset (XXX)
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX"),
        // ISO 8601 with microseconds and Z
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'").withZone(java.time.ZoneOffset.UTC),
        // ISO 8601 with microseconds and timezone offset
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX"),
        // ISO 8601 with nanoseconds and Z
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSSSSS'Z'").withZone(java.time.ZoneOffset.UTC),
        // ISO 8601 with nanoseconds and timezone offset
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSSSSSXXX")
    };

    @Override
    public Instant deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String dateString = p.getText();
        if (dateString == null || dateString.trim().isEmpty()) {
            return null;
        }

        // Try parsing with each formatter
        for (DateTimeFormatter formatter : FORMATTERS) {
            try {
                return Instant.from(formatter.parse(dateString));
            } catch (DateTimeParseException e) {
                // Continue to next formatter
            }
        }

        // If all formatters fail, try ISO_INSTANT (most permissive)
        try {
            return Instant.parse(dateString);
        } catch (DateTimeParseException e) {
            throw new IOException("Unable to parse date: " + dateString + ". Supported formats: " +
                    "yyyy-MM-dd'T'HH:mm:ssZ, yyyy-MM-dd'T'HH:mm:ssXXX, " +
                    "yyyy-MM-dd'T'HH:mm:ss.SSSZ, yyyy-MM-dd'T'HH:mm:ss.SSSXXX", e);
        }
    }
}
