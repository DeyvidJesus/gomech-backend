package com.gomech.service;

import com.gomech.domain.InventoryItem;
import com.gomech.domain.Part;
import com.gomech.dto.ServiceOrder.ServiceOrderItemCreateDTO;
import com.gomech.model.ServiceOrder;
import com.gomech.model.ServiceOrderItem;
import com.gomech.model.ServiceOrderItemType;
import com.gomech.repository.InventoryItemRepository;
import com.gomech.repository.PartRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Objects;

@Component
public class ServiceOrderItemAssembler {

    private final PartRepository partRepository;
    private final InventoryItemRepository inventoryItemRepository;

    public ServiceOrderItemAssembler(PartRepository partRepository,
                                     InventoryItemRepository inventoryItemRepository) {
        this.partRepository = partRepository;
        this.inventoryItemRepository = inventoryItemRepository;
    }

    public ServiceOrderItem create(ServiceOrder serviceOrder, ServiceOrderItemCreateDTO dto) {
        ServiceOrderItem item = new ServiceOrderItem();
        item.setServiceOrder(serviceOrder);
        applyCommonValues(item, dto, true);
        ensureDescription(item);
        return item;
    }

    public void update(ServiceOrderItem item, ServiceOrderItemCreateDTO dto) {
        applyCommonValues(item, dto, false);
        ensureDescription(item);
    }

    private void applyCommonValues(ServiceOrderItem item, ServiceOrderItemCreateDTO dto, boolean isCreate) {
        if (dto.getItemType() != null) {
            item.setItemType(dto.getItemType());
        } else if (isCreate && item.getItemType() == null) {
            item.setItemType(ServiceOrderItemType.PART);
        }

        if (dto.getQuantity() != null) {
            item.setQuantity(dto.getQuantity());
        } else if (isCreate && item.getQuantity() == null) {
            item.setQuantity(1);
        }

        if (dto.getUnitPrice() != null) {
            item.setUnitPrice(dto.getUnitPrice());
        } else if (isCreate && item.getUnitPrice() == null) {
            item.setUnitPrice(BigDecimal.ZERO);
        }

        Part previousPart = item.getPart();
        resolvePart(dto, item);
        InventoryItem resolvedInventoryItem = resolveInventoryItem(dto, item);
        Part currentPart = item.getPart();
        boolean partChanged = currentPart != null && (previousPart == null
                || !Objects.equals(previousPart.getId(), currentPart.getId()));

        if (dto.getDescription() != null) {
            item.setDescription(dto.getDescription());
        } else if (currentPart != null && (isCreate || partChanged || item.getDescription() == null)) {
            item.setDescription(currentPart.getName());
        }

        if (dto.getProductCode() != null) {
            item.setProductCode(dto.getProductCode());
        } else if (currentPart != null && (isCreate || partChanged || item.getProductCode() == null)) {
            item.setProductCode(currentPart.getSku());
        }

        if (dto.getUnitPrice() == null && currentPart != null && currentPart.getUnitPrice() != null
                && (isCreate || partChanged || item.getUnitPrice() == null
                || BigDecimal.ZERO.compareTo(item.getUnitPrice()) == 0)) {
            item.setUnitPrice(currentPart.getUnitPrice());
        }

        if (dto.getObservations() != null) {
            item.setObservations(dto.getObservations());
        }

        if (resolvedInventoryItem != null) {
            item.setInventoryItem(resolvedInventoryItem);
            item.setRequiresStock(true);
        } else if (dto.getRequiresStock() != null) {
            item.setRequiresStock(dto.getRequiresStock());
        } else if (isCreate && item.getRequiresStock() == null) {
            item.setRequiresStock(false);
        }
    }

    private Part resolvePart(ServiceOrderItemCreateDTO dto, ServiceOrderItem item) {
        if (dto.getPartId() == null) {
            return item.getPart();
        }

        Part part = partRepository.findById(dto.getPartId())
                .orElseThrow(() -> new IllegalArgumentException("Peça não encontrada"));
        item.setPart(part);
        return part;
    }

    private InventoryItem resolveInventoryItem(ServiceOrderItemCreateDTO dto, ServiceOrderItem item) {
        if (dto.getInventoryItemId() == null) {
            return item.getInventoryItem();
        }

        InventoryItem inventoryItem = inventoryItemRepository.findById(dto.getInventoryItemId())
                .orElseThrow(() -> new IllegalArgumentException("Item de estoque não encontrado"));
        item.setInventoryItem(inventoryItem);

        Part inventoryPart = inventoryItem.getPart();
        if (inventoryPart != null && !Objects.equals(item.getPart(), inventoryPart)) {
            item.setPart(inventoryPart);
        }
        return inventoryItem;
    }

    private void ensureDescription(ServiceOrderItem item) {
        if (item.getDescription() == null || item.getDescription().isBlank()) {
            throw new IllegalArgumentException("Descrição do item é obrigatória");
        }
    }
}
