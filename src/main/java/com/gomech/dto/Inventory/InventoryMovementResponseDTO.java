package com.gomech.dto.Inventory;

import com.gomech.domain.InventoryMovement;
import com.gomech.domain.InventoryMovementType;

import java.time.LocalDateTime;

public record InventoryMovementResponseDTO(
        Long id,
        Long inventoryItemId,
        Long partId,
        String partName,
        InventoryMovementType movementType,
        Integer quantity,
        String referenceCode,
        String notes,
        Long serviceOrderId,
        Long vehicleId,
        LocalDateTime movementDate
) {
    public static InventoryMovementResponseDTO fromEntity(InventoryMovement movement) {
        return new InventoryMovementResponseDTO(
                movement.getId(),
                movement.getInventoryItem() != null ? movement.getInventoryItem().getId() : null,
                movement.getPart() != null ? movement.getPart().getId() : null,
                movement.getPart() != null ? movement.getPart().getName() : null,
                movement.getMovementType(),
                movement.getQuantity(),
                movement.getReferenceCode(),
                movement.getNotes(),
                movement.getServiceOrder() != null ? movement.getServiceOrder().getId() : null,
                movement.getVehicle() != null ? movement.getVehicle().getId() : null,
                movement.getMovementDate()
        );
    }
}
