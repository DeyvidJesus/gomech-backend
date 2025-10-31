package com.gomech.dto.Inventory;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record InventoryEntryRequestDTO(
        @NotNull Long partId,
        @NotBlank String location,
        @NotNull @Min(1) Integer quantity,
        BigDecimal unitCost,
        BigDecimal salePrice,
        String referenceCode,
        String notes
) {
}
