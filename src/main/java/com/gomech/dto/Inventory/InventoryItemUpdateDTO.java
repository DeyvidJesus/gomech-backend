package com.gomech.dto.Inventory;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record InventoryItemUpdateDTO(
        @Size(max = 100) String location,
        @Min(0) Integer minimumQuantity,
        BigDecimal unitCost,
        BigDecimal salePrice
) {
}
