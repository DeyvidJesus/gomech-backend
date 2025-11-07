package com.gomech.dto.Parts;

import com.gomech.domain.Part;

public final class PartMapper {

    private PartMapper() {
    }

    public static Part toEntity(PartCreateDTO dto) {
        Part part = new Part();
        part.setName(dto.name());
        // SKU será definido no serviço se não for fornecido
        if (dto.sku() != null && !dto.sku().trim().isEmpty()) {
            part.setSku(dto.sku());
        }
        part.setManufacturer(dto.manufacturer());
        part.setDescription(dto.description());
        part.setUnitCost(dto.unitCost());
        part.setUnitPrice(dto.unitPrice());
        part.setActive(dto.active());
        return part;
    }

    public static void updateEntity(Part part, PartUpdateDTO dto) {
        if (dto.name() != null) {
            part.setName(dto.name());
        }
        if (dto.sku() != null) {
            part.setSku(dto.sku());
        }
        if (dto.manufacturer() != null) {
            part.setManufacturer(dto.manufacturer());
        }
        if (dto.description() != null) {
            part.setDescription(dto.description());
        }
        if (dto.unitCost() != null) {
            part.setUnitCost(dto.unitCost());
        }
        if (dto.unitPrice() != null) {
            part.setUnitPrice(dto.unitPrice());
        }
        if (dto.active() != null) {
            part.setActive(dto.active());
        }
    }
}
