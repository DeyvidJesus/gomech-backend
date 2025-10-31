package com.gomech.dto.Audit;

import jakarta.validation.constraints.NotBlank;

public record AuditEventRequest(@NotBlank String eventType, @NotBlank String payload) {
}
