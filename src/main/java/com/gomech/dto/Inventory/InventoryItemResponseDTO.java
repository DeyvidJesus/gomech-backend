package com.gomech.dto.Inventory;

import com.gomech.domain.InventoryItem;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record InventoryItemResponseDTO(
        Long id,
        Long partId,
        String partName,
        String partSku,
        String location,
        Integer quantity,
        Integer reservedQuantity,
        Integer minimumQuantity,
        BigDecimal unitCost,
        BigDecimal salePrice,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static InventoryItemResponseDTO fromEntity(InventoryItem item) {
        return new InventoryItemResponseDTO(
                item.getId(),
                item.getPart() != null ? item.getPart().getId() : null,
                item.getPart() != null ? item.getPart().getName() : null,
                item.getPart() != null ? item.getPart().getSku() : null,
                item.getLocation(),
                item.getQuantity(),
                item.getReservedQuantity(),
                item.getMinimumQuantity(),
                item.getUnitCost(),
                item.getSalePrice(),
                item.getCreatedAt(),
                item.getUpdatedAt()
        );
    }
}
