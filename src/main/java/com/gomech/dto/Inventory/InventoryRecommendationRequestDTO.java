package com.gomech.dto.Inventory;

/**
 * Representa o payload enviado para o backend solicitar recomendações de peças.
 */
public record InventoryRecommendationRequestDTO(
        Long vehicleId,
        Long serviceOrderId,
        Integer limit
) {
}
