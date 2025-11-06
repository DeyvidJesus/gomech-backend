package com.gomech.service;

import com.gomech.domain.InventoryItem;
import com.gomech.dto.ServiceOrder.ServiceOrderItemCreateDTO;
import com.gomech.dto.ServiceOrder.ServiceOrderItemResponseDTO;
import com.gomech.model.ServiceOrder;
import com.gomech.model.ServiceOrderItem;
import com.gomech.model.ServiceOrderItemType;
import com.gomech.repository.ServiceOrderItemRepository;
import com.gomech.repository.ServiceOrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class ServiceOrderItemService {
    private static final Logger logger = LoggerFactory.getLogger(ServiceOrderItemService.class);

    @Autowired
    private ServiceOrderItemRepository itemRepository;
    
    @Autowired
    private ServiceOrderRepository serviceOrderRepository;

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private ServiceOrderItemAssembler serviceOrderItemAssembler;

    public ServiceOrderItemResponseDTO addItem(Long serviceOrderId, ServiceOrderItemCreateDTO dto) {
        ServiceOrder serviceOrder = serviceOrderRepository.findById(serviceOrderId)
            .orElseThrow(() -> new RuntimeException("Ordem de serviço não encontrada"));
        logger.info("Service Order: {}", serviceOrder.toString());
        logger.info("Service Order Item: {}", dto);
        logger.info("Service Order ID: {}", serviceOrderId);

        ServiceOrderItem item = serviceOrderItemAssembler.create(serviceOrder, dto);
        ServiceOrderItem saved = itemRepository.save(item);
        logger.info("Service Order Item Saved: {}", saved.toString());
        logger.info("Service Order To Be Saved: {}", serviceOrder.toString());

        if (Boolean.TRUE.equals(saved.getRequiresStock())) {
            saved.apply();
            inventoryService.consumeDirect(serviceOrder, saved, saved.getQuantity(),
                    "Consumo automático ao adicionar item");
            saved = itemRepository.save(saved);
        }

        // Recalcular custos da OS
        serviceOrder.calculateTotalCost();
        serviceOrderRepository.save(serviceOrder);
        
        return convertToResponseDTO(saved);
    }

    public List<ServiceOrderItemResponseDTO> getItemsByServiceOrder(Long serviceOrderId) {
        return itemRepository.findByServiceOrderId(serviceOrderId).stream()
                .map(this::convertToResponseDTO)
                .collect(Collectors.toList());
    }

    public List<ServiceOrderItemResponseDTO> getItemsByType(ServiceOrderItemType itemType) {
        return itemRepository.findByItemType(itemType).stream()
                .map(this::convertToResponseDTO)
                .collect(Collectors.toList());
    }

    public Optional<ServiceOrderItemResponseDTO> getById(Long id) {
        return itemRepository.findById(id)
                .map(this::convertToResponseDTO);
    }

    public ServiceOrderItemResponseDTO updateItem(Long id, ServiceOrderItemCreateDTO dto) {
        ServiceOrderItem item = itemRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Item não encontrado"));

        boolean previousRequiresStock = Boolean.TRUE.equals(item.getRequiresStock());
        boolean wasApplied = Boolean.TRUE.equals(item.getApplied());
        int previousQuantity = item.getQuantity();
        InventoryItem previousInventoryItem = item.getInventoryItem();
        Long previousInventoryItemId = previousInventoryItem != null ? previousInventoryItem.getId() : null;
        Long newInventoryItemId = dto.getInventoryItemId();
        boolean inventoryItemWillChange = newInventoryItemId != null && !Objects.equals(previousInventoryItemId, newInventoryItemId);

        serviceOrderItemAssembler.update(item, dto);
        ServiceOrderItem updated = itemRepository.save(item);

        adjustInventoryOnUpdate(updated, previousRequiresStock, wasApplied, previousQuantity, previousInventoryItem, inventoryItemWillChange);

        if (!Boolean.TRUE.equals(updated.getRequiresStock())) {
            updated.setInventoryItem(null);
            itemRepository.save(updated);
        }

        // Recalcular custos da OS
        ServiceOrder serviceOrder = updated.getServiceOrder();
        serviceOrder.calculateTotalCost();
        serviceOrderRepository.save(serviceOrder);
        
        return convertToResponseDTO(updated);
    }

    public void deleteItem(Long id) {
        ServiceOrderItem item = itemRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Item não encontrado"));
        
        ServiceOrder serviceOrder = item.getServiceOrder();
        handleInventoryOnDelete(item);
        itemRepository.deleteById(id);
        
        // Recalcular custos da OS
        serviceOrder.calculateTotalCost();
        serviceOrderRepository.save(serviceOrder);
    }

    public ServiceOrderItemResponseDTO applyItem(Long id) {
        ServiceOrderItem item = itemRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Item não encontrado"));

        if (Boolean.TRUE.equals(item.getRequiresStock())) {
            if (Boolean.TRUE.equals(item.getApplied())) {
                return convertToResponseDTO(item);
            }
            inventoryService.consumeDirect(item.getServiceOrder(), item, item.getQuantity(),
                    "Baixa por aplicação de item");
        }

        item.apply();
        ServiceOrderItem updated = itemRepository.save(item);
        
        // Recalcular custos da OS ao aplicar item
        ServiceOrder serviceOrder = updated.getServiceOrder();
        serviceOrder.calculateTotalCost();
        serviceOrderRepository.save(serviceOrder);
        
        return convertToResponseDTO(updated);
    }

    public ServiceOrderItemResponseDTO unapplyItem(Long id) {
        ServiceOrderItem item = itemRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Item não encontrado"));

        if (Boolean.TRUE.equals(item.getRequiresStock()) && Boolean.TRUE.equals(item.getApplied())) {
            inventoryService.returnToStock(item.getServiceOrder(), item, item.getQuantity(),
                    "Devolução por desaplicação de item");
        }

        item.unapply();
        ServiceOrderItem updated = itemRepository.save(item);
        
        // Recalcular custos da OS ao desaplicar item
        ServiceOrder serviceOrder = updated.getServiceOrder();
        serviceOrder.calculateTotalCost();
        serviceOrderRepository.save(serviceOrder);
        
        return convertToResponseDTO(updated);
    }

    // Métodos para futuro controle de estoque
    public List<ServiceOrderItemResponseDTO> getPartsRequiringStock() {
        return itemRepository.findPartsRequiringStock().stream()
                .map(this::convertToResponseDTO)
                .collect(Collectors.toList());
    }

    public List<ServiceOrderItemResponseDTO> getReservedNotAppliedItems() {
        return itemRepository.findReservedNotAppliedItems().stream()
                .map(this::convertToResponseDTO)
                .collect(Collectors.toList());
    }

    public ServiceOrderItemResponseDTO consumeStock(Long id) {
        ServiceOrderItem item = itemRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Item não encontrado"));

        item.apply();
        inventoryService.consumeDirect(item.getServiceOrder(), item, item.getQuantity(),
                "Consumo manual de estoque");
        ServiceOrderItem updated = itemRepository.save(item);
        return convertToResponseDTO(updated);
    }

    public ServiceOrderItemResponseDTO returnStock(Long id) {
        ServiceOrderItem item = itemRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Item não encontrado"));

        if (Boolean.TRUE.equals(item.getApplied())) {
            inventoryService.returnToStock(item.getServiceOrder(), item, item.getQuantity(),
                    "Devolução manual de item aplicado");
            item.unapply();
        }

        ServiceOrderItem updated = itemRepository.save(item);
        return convertToResponseDTO(updated);
    }

    private ServiceOrderItemResponseDTO convertToResponseDTO(ServiceOrderItem item) {
        ServiceOrderItemResponseDTO dto = new ServiceOrderItemResponseDTO();
        dto.setId(item.getId());
        dto.setDescription(item.getDescription());
        dto.setItemType(item.getItemType());
        dto.setQuantity(item.getQuantity());
        dto.setUnitPrice(item.getUnitPrice());
        dto.setTotalPrice(item.getTotalPrice());
        dto.setProductCode(item.getProductCode());
        if (item.getPart() != null) {
            dto.setPartId(item.getPart().getId());
            dto.setPartName(item.getPart().getName());
            dto.setPartSku(item.getPart().getSku());
        }
        if (item.getInventoryItem() != null) {
            dto.setInventoryItemId(item.getInventoryItem().getId());
            dto.setInventoryLocation(item.getInventoryItem().getLocation());
        }
        dto.setRequiresStock(item.getRequiresStock());
        dto.setStockReserved(item.getStockReserved());
        dto.setApplied(item.getApplied());
        dto.setObservations(item.getObservations());
        dto.setCreatedAt(item.getCreatedAt());
        dto.setUpdatedAt(item.getUpdatedAt());
        return dto;
    }

    private void adjustInventoryOnUpdate(ServiceOrderItem updated,
                                         boolean previousRequiresStock,
                                         boolean wasApplied,
                                         int previousQuantity,
                                         InventoryItem previousInventoryItem,
                                         boolean inventoryItemChanged) {
        boolean currentRequiresStock = Boolean.TRUE.equals(updated.getRequiresStock());
        ServiceOrder serviceOrder = updated.getServiceOrder();

        if (!previousRequiresStock && currentRequiresStock && Boolean.TRUE.equals(updated.getApplied())) {
            inventoryService.consumeDirect(serviceOrder, updated, updated.getQuantity(),
                    "Consumo ao atualizar item para exigir estoque");
            return;
        }

        if (previousRequiresStock && !currentRequiresStock) {
            if (wasApplied && previousInventoryItem != null) {
                InventoryItem newInventoryItem = updated.getInventoryItem();
                updated.setInventoryItem(previousInventoryItem);
                inventoryService.returnToStock(serviceOrder, updated, previousQuantity,
                        "Devolução ao remover controle de estoque");
                updated.unapply();
                updated.setInventoryItem(newInventoryItem);
                itemRepository.save(updated);
            }
            return;
        }

        if (!currentRequiresStock || !wasApplied || previousInventoryItem == null) {
            return;
        }

        if (inventoryItemChanged) {
            InventoryItem newInventoryItem = updated.getInventoryItem();
            updated.setInventoryItem(previousInventoryItem);
            inventoryService.returnToStock(serviceOrder, updated, previousQuantity,
                    "Devolução por alteração de item de estoque");
            updated.setInventoryItem(newInventoryItem);
            inventoryService.consumeDirect(serviceOrder, updated, updated.getQuantity(),
                    "Consumo por alteração de item de estoque");
            return;
        }

        int difference = updated.getQuantity() - previousQuantity;
        if (difference > 0) {
            inventoryService.consumeDirect(serviceOrder, updated, difference,
                    "Consumo adicional por aumento de quantidade");
        } else if (difference < 0) {
            inventoryService.returnToStock(serviceOrder, updated, Math.abs(difference),
                    "Devolução parcial por redução de quantidade");
        }
    }

    private void handleInventoryOnDelete(ServiceOrderItem item) {
        if (!Boolean.TRUE.equals(item.getRequiresStock())) {
            return;
        }

        ServiceOrder serviceOrder = item.getServiceOrder();
        if (Boolean.TRUE.equals(item.getApplied())) {
            inventoryService.returnToStock(serviceOrder, item, item.getQuantity(),
                    "Devolução por remoção de item aplicado");
            item.unapply();
        } else if (Boolean.TRUE.equals(item.getStockReserved())) {
            inventoryService.cancelReservation(serviceOrder, item, item.getQuantity(),
                    "Cancelamento de reserva por remoção de item");
        }
    }
}
