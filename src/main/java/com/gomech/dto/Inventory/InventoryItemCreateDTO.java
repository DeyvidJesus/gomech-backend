package com.gomech.dto.Inventory;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record InventoryItemCreateDTO(
        @NotNull Long partId,
        @NotBlank @Size(max = 100) String location,
        @Min(0) Integer initialQuantity,
        BigDecimal unitCost,
        BigDecimal salePrice
) {
}
