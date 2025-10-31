package com.gomech.dto.Parts;

import com.gomech.domain.Part;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PartResponseDTO(
        Long id,
        String name,
        String sku,
        String manufacturer,
        String description,
        BigDecimal unitCost,
        BigDecimal unitPrice,
        Boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static PartResponseDTO fromEntity(Part part) {
        return new PartResponseDTO(
                part.getId(),
                part.getName(),
                part.getSku(),
                part.getManufacturer(),
                part.getDescription(),
                part.getUnitCost(),
                part.getUnitPrice(),
                part.getActive(),
                part.getCreatedAt(),
                part.getUpdatedAt()
        );
    }
}
