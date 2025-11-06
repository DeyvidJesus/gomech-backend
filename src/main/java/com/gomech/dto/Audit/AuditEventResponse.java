package com.gomech.dto.Audit;

import com.gomech.model.AuditEvent;

import java.time.Instant;
import java.time.LocalDateTime;

public record AuditEventResponse(
        Long id,
        String eventType,
        String operation,
        String userEmail,
        String moduleName,
        String userRole,
        LocalDateTime occurredAt,
        Instant createdAt,
        String blockchainReference,
        String eventHash
) {
    public static AuditEventResponse fromEntity(AuditEvent event) {
        return new AuditEventResponse(
                event.getId(),
                event.getEventType(),
                event.getOperation(),
                event.getUserEmail(),
                event.getModuleName(),
                event.getUserRole(),
                event.getOccurredAt(),
                event.getCreatedAt(),
                event.getBlockchainReference(),
                event.getEventHash()
        );
    }
}
