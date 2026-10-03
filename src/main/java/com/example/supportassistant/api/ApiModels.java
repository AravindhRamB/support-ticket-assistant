package com.example.supportassistant.api;

import com.example.supportassistant.domain.AnalysisStatus;
import com.example.supportassistant.domain.Priority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.UUID;

public final class ApiModels {
    private ApiModels() {}

    public record CreateTicketRequest(
            @NotBlank(message = "customerId is required")
            @Size(max = 100, message = "customerId must be at most 100 characters")
            String customerId,

            @NotBlank(message = "subject is required")
            @Size(max = 300, message = "subject must be at most 300 characters")
            String subject,

            @NotBlank(message = "description is required")
            @Size(max = 10000, message = "description must be at most 10000 characters")
            String description,

            @NotNull(message = "priority is required")
            Priority priority,

            @Size(max = 150, message = "product must be at most 150 characters")
            String product
    ) {}

    public record AnalysisResponse(
            String category,
            String summary,
            String suggestedResponse,
            String recommendedTeam,
            double confidence
    ) {}

    public record TicketResponse(
            UUID ticketId,
            String customerId,
            String subject,
            String description,
            Priority priority,
            String product,
            AnalysisStatus status,
            AnalysisResponse analysis,
            String analysisError,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt
    ) {}

    public record TicketSummaryResponse(
            UUID ticketId,
            String customerId,
            String subject,
            Priority priority,
            String product,
            AnalysisStatus status,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt
    ) {}

    public record PageResponse<T>(
            java.util.List<T> content,
            int page,
            int size,
            long totalElements,
            int totalPages
    ) {}

    public record ErrorResponse(
            OffsetDateTime timestamp,
            int status,
            String error,
            String message,
            String path
    ) {}
}
