package com.gomech.dto.Inventory;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record StockCancellationRequestDTO(
        @NotNull Long serviceOrderItemId,
        @NotNull @Min(1) Integer quantity,
        String notes
) {
}
