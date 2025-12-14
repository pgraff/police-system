package com.knowit.policesystem.edge.controllers;

import com.knowit.policesystem.edge.services.ProjectionQueryProxyService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for querying projections.
 * Provides endpoints to query projection services through the edge with filtering, pagination, and sorting.
 */
@RestController
@RequestMapping("/api/v1/query")
public class QueryController extends BaseRestController {

    private final ProjectionQueryProxyService queryProxyService;

    public QueryController(ProjectionQueryProxyService queryProxyService) {
        this.queryProxyService = queryProxyService;
    }

    /**
     * Gets a full resource from projections.
     * Aggregates data from multiple projections if needed.
     *
     * @param domain the domain name (e.g., "incident", "officer", "call")
     * @param id the resource identifier
     * @param fields optional comma-separated list of fields to include
     * @return 200 OK with resource data
     */
    @GetMapping("/{domain}/{id}/full")
    public ResponseEntity<com.knowit.policesystem.edge.dto.SuccessResponse<Object>> getFullResource(
            @PathVariable String domain,
            @PathVariable String id,
            @RequestParam(required = false) String fields) {

        Object resource = queryProxyService.getFullResource(domain, id);
        if (resource == null) {
            return ResponseEntity.notFound().build();
        }
        
        // TODO: Apply field selection if fields parameter is provided
        // This would require projection services to support field selection
        
        return success(resource, "Resource retrieved");
    }

    /**
     * Checks if a resource exists.
     *
     * @param domain the domain name
     * @param id the resource identifier
     * @return 200 OK with existence status
     */
    @GetMapping("/{domain}/{id}/exists")
    public ResponseEntity<com.knowit.policesystem.edge.dto.SuccessResponse<Map<String, Boolean>>> exists(
            @PathVariable String domain,
            @PathVariable String id) {

        boolean exists = queryProxyService.exists(domain, id);
        return success(Map.of("exists", exists), "Existence check completed");
    }

    /**
     * Lists resources with filtering, pagination, and sorting.
     * Note: This is a placeholder - actual implementation would require projection services to support these features.
     *
     * @param domain the domain name
     * @param status optional status filter
     * @param priority optional priority filter
     * @param page page number (0-based, default 0)
     * @param size page size (default 20)
     * @param sort sort field and direction (e.g., "id,asc" or "createdAt,desc")
     * @return 200 OK with paginated results
     */
    @GetMapping("/{domain}")
    public ResponseEntity<com.knowit.policesystem.edge.dto.SuccessResponse<Map<String, Object>>> listResources(
            @PathVariable String domain,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort) {

        // Parse sort parameter
        Sort sortObj = Sort.unsorted();
        if (sort != null && !sort.isEmpty()) {
            String[] sortParts = sort.split(",");
            if (sortParts.length == 2) {
                Sort.Direction direction = "desc".equalsIgnoreCase(sortParts[1]) 
                        ? Sort.Direction.DESC 
                        : Sort.Direction.ASC;
                sortObj = Sort.by(direction, sortParts[0]);
            }
        }

        Pageable pageable = PageRequest.of(page, size, sortObj);

        // TODO: Implement actual querying with filters
        // This would require projection services to support query parameters
        // For now, return a placeholder response
        Map<String, Object> result = Map.of(
                "content", List.of(),
                "totalElements", 0,
                "totalPages", 0,
                "page", page,
                "size", size
        );

        return success(result, "Resources retrieved");
    }
}
