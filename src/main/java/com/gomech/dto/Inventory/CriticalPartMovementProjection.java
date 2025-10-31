package com.gomech.dto.Inventory;

import java.time.LocalDateTime;

public record CriticalPartMovementProjection(
        Long partId,
        String vehicleModel,
        Long totalConsumed,
        LocalDateTime lastMovementDate
) {
}
