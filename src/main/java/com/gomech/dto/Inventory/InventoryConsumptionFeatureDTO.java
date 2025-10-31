package com.gomech.dto.Inventory;

import java.time.LocalDateTime;

/**
 * Representa uma feature de consumo de peças que pode ser enviada para o motor de IA.
 */
public record InventoryConsumptionFeatureDTO(
        Long partId,
        String partName,
        String partSku,
        Long totalQuantity,
        Long distinctServiceOrders,
        Long distinctVehicles,
        LocalDateTime lastMovementDate,
        Long vehicleId,
        Long serviceOrderId
) {
}
