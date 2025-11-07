package com.gomech.dto.Analytics;

import java.time.LocalDateTime;

public record ClientServiceGap(
        Long clientId,
        String clientName,
        String clientEmail,
        LocalDateTime lastServiceDate
) {
}
