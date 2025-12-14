package com.knowit.policesystem.edge.dto;

import java.util.List;

/**
 * Response DTO for batch operations.
 * Contains lists of successfully created and failed items.
 */
public class BatchCreateResponseDto {

    private List<String> created;
    private List<BatchFailure> failed;

    /**
     * Default constructor for Jackson serialization.
     */
    public BatchCreateResponseDto() {
    }

    /**
     * Creates a new batch create response DTO.
     *
     * @param created list of successfully created IDs
     * @param failed list of failures with reasons
     */
    public BatchCreateResponseDto(List<String> created, List<BatchFailure> failed) {
        this.created = created;
        this.failed = failed;
    }

    public List<String> getCreated() {
        return created;
    }

    public void setCreated(List<String> created) {
        this.created = created;
    }

    public List<BatchFailure> getFailed() {
        return failed;
    }

    public void setFailed(List<BatchFailure> failed) {
        this.failed = failed;
    }

    /**
     * Represents a failed batch item.
     */
    public static class BatchFailure {
        private String id;
        private String reason;

        public BatchFailure() {
        }

        public BatchFailure(String id, String reason) {
            this.id = id;
            this.reason = reason;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }
    }
}
