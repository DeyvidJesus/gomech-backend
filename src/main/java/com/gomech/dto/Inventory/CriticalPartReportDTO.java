package com.gomech.dto.Inventory;

import java.time.LocalDateTime;

public record CriticalPartReportDTO(
        Long partId,
        String partName,
        String partSku,
        String vehicleModel,
        Long totalQuantity,
        Long reservedQuantity,
        Long minimumQuantity,
        Long availableQuantity,
        Long totalConsumed,
        LocalDateTime lastMovementDate
) {
}
