package com.gomech.dto.Audit;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;

public record AuditEventRequest(
        @NotBlank String eventType,
        @NotBlank String operation,
        @NotBlank String userEmail,
        @NotBlank String moduleName,
        @NotBlank String userRole,
        LocalDateTime occurredAt,
        String metadata,
        Long entityId
) {
}
