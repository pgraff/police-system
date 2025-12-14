package com.knowit.policesystem.edge.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.knowit.policesystem.edge.model.IdempotencyRecord;
import com.knowit.policesystem.edge.services.IdempotencyService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * Filter that handles idempotency keys.
 * Intercepts requests with Idempotency-Key header and returns cached responses for duplicates.
 */
@Component
@Order(1)
public class IdempotencyFilter extends OncePerRequestFilter {

    private static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";
    private final IdempotencyService idempotencyService;
    private final ObjectMapper objectMapper;

    public IdempotencyFilter(IdempotencyService idempotencyService, ObjectMapper objectMapper) {
        this.idempotencyService = idempotencyService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        
        String idempotencyKey = request.getHeader(IDEMPOTENCY_KEY_HEADER);
        
        if (idempotencyKey == null || idempotencyKey.trim().isEmpty()) {
            // No idempotency key, proceed normally
            filterChain.doFilter(request, response);
            return;
        }

        // Only handle POST, PUT, PATCH requests
        String method = request.getMethod();
        if (!"POST".equals(method) && !"PUT".equals(method) && !"PATCH".equals(method)) {
            filterChain.doFilter(request, response);
            return;
        }

        String endpoint = request.getRequestURI();
        Optional<IdempotencyRecord> cachedRecord = idempotencyService.findCachedResponse(idempotencyKey, endpoint);

        if (cachedRecord.isPresent()) {
            // Return cached response
            IdempotencyRecord record = cachedRecord.get();
            response.setStatus(record.getHttpStatus());
            response.setContentType("application/json");
            response.getWriter().write(record.getResponse());
            return;
        }

        // Wrap request and response to capture body
        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);

        try {
            filterChain.doFilter(wrappedRequest, wrappedResponse);
            
            // Store response for future idempotent requests
            String requestBody = new String(wrappedRequest.getContentAsByteArray(), StandardCharsets.UTF_8);
            String responseBody = new String(wrappedResponse.getContentAsByteArray(), StandardCharsets.UTF_8);
            idempotencyService.storeResponse(idempotencyKey, endpoint, requestBody, responseBody, wrappedResponse.getStatus());
            
            // Copy response to actual response
            wrappedResponse.copyBodyToResponse();
        } catch (Exception e) {
            // Don't cache error responses
            wrappedResponse.copyBodyToResponse();
            throw e;
        }
    }
}
