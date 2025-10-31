package com.gomech.dto.Inventory;

import java.time.LocalDateTime;

public record PartAvailabilityDTO(
        Long partId,
        String partName,
        String partSku,
        Long totalQuantity,
        Long reservedQuantity,
        Long minimumQuantity,
        Long availableQuantity,
        LocalDateTime lastMovementDate
) {
}
