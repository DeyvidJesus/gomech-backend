package com.gomech.dto.Inventory;

import java.time.LocalDateTime;

/**
 * Projeção utilizada para calcular estatísticas locais de consumo de peças.
 */
public record PartConsumptionStats(
        Long partId,
        String partName,
        String partSku,
        Long totalQuantity,
        Long distinctServiceOrders,
        Long distinctVehicles,
        LocalDateTime lastMovementDate
) {
}
