package com.gomech.dto.Inventory;

import java.time.LocalDateTime;

/**
 * Representa uma recomendação de peça retornada pelo motor de IA ou pelo fallback local.
 */
public record InventoryRecommendationDTO(
        Long partId,
        String partName,
        String partSku,
        double confidence,
        String rationale,
        boolean fromFallback,
        long historicalQuantity,
        LocalDateTime lastMovementDate
) {
}
