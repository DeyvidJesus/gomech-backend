package com.gomech.dto.Parts;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record PartCreateDTO(
        @NotBlank @Size(max = 150) String name,
        @NotBlank @Size(max = 100) String sku,
        @Size(max = 150) String manufacturer,
        String description,
        @DecimalMin(value = "0.0") BigDecimal unitCost,
        @DecimalMin(value = "0.0") BigDecimal unitPrice,
        @NotNull Boolean active
) {
}
