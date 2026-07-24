package com.deckflow.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import java.time.Instant;

public final class ApiDtos {
    private ApiDtos() {}

    public record CreateSessionRequest(
            @Min(3) @Max(30) int pageCount,
            @NotBlank String requirement
    ) {}

    public record ChatRequest(@NotBlank String message) {}

    public record ChatResponse(String reply, boolean canGenerate) {}

    public record SessionSummary(
            String id,
            String title,
            int pageCount,
            String status,
            Instant updatedAt,
            int versionCount
    ) {}

    public record GenerateResponse(
            String sessionId,
            int version,
            String fileName,
            String downloadUrl
    ) {}
}

