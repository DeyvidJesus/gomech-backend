package com.gomech.service;

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
            inventoryService.reserveStock(serviceOrder, saved, saved.getQuantity(),
                    "Reserva automática ao adicionar item");
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
        int previousQuantity = item.getQuantity();
        Long previousInventoryItemId = item.getInventoryItem() != null ? item.getInventoryItem().getId() : null;
        Long newInventoryItemId = dto.getInventoryItemId();
        boolean inventoryItemWillChange = newInventoryItemId != null && !Objects.equals(previousInventoryItemId, newInventoryItemId);

        if (inventoryItemWillChange && previousRequiresStock) {
            handleInventoryOnDelete(item);
            item.setInventoryItem(null);
        }

        serviceOrderItemAssembler.update(item, dto);
        ServiceOrderItem updated = itemRepository.save(item);

        adjustInventoryOnUpdate(updated, previousRequiresStock, previousQuantity, inventoryItemWillChange);

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
            inventoryService.consumeStock(item.getServiceOrder(), item, item.getQuantity(),
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

        if (Boolean.TRUE.equals(item.getRequiresStock())) {
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

    public ServiceOrderItemResponseDTO reserveStock(Long id) {
        ServiceOrderItem item = itemRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Item não encontrado"));

        inventoryService.reserveStock(item.getServiceOrder(), item, item.getQuantity(),
                "Reserva manual de item");
        ServiceOrderItem updated = itemRepository.save(item);
        return convertToResponseDTO(updated);
    }

    public ServiceOrderItemResponseDTO releaseStock(Long id) {
        ServiceOrderItem item = itemRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Item não encontrado"));

        if (Boolean.TRUE.equals(item.getApplied())) {
            inventoryService.returnToStock(item.getServiceOrder(), item, item.getQuantity(),
                    "Devolução manual de item aplicado");
            item.unapply();
        } else {
            inventoryService.cancelReservation(item.getServiceOrder(), item, item.getQuantity(),
                    "Liberação manual de reserva");
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
                                        int previousQuantity,
                                        boolean inventoryItemChanged) {
        boolean currentRequiresStock = Boolean.TRUE.equals(updated.getRequiresStock());
        int currentQuantity = updated.getQuantity();
        ServiceOrder serviceOrder = updated.getServiceOrder();

        if (currentRequiresStock && !previousRequiresStock) {
            inventoryService.reserveStock(serviceOrder, updated, currentQuantity,
                    "Reserva ao atualizar item para exigir estoque");
            return;
        }

        if (!currentRequiresStock && previousRequiresStock) {
            if (Boolean.TRUE.equals(updated.getApplied())) {
                inventoryService.returnToStock(serviceOrder, updated, previousQuantity,
                        "Devolução ao remover controle de estoque de item aplicado");
                updated.unapply();
            } else if (Boolean.TRUE.equals(updated.getStockReserved())) {
                inventoryService.cancelReservation(serviceOrder, updated, previousQuantity,
                        "Cancelamento de reserva ao remover controle de estoque");
            }
            return;
        }

        if (currentRequiresStock && inventoryItemChanged) {
            inventoryService.reserveStock(serviceOrder, updated, currentQuantity,
                    "Reserva ao alterar item de estoque");
            return;
        }

        if (currentRequiresStock) {
            int difference = currentQuantity - previousQuantity;
            if (difference > 0) {
                inventoryService.reserveStock(serviceOrder, updated, difference,
                        "Reserva adicional por aumento de quantidade");
            } else if (difference < 0) {
                int release = Math.abs(difference);
                if (Boolean.TRUE.equals(updated.getApplied())) {
                    inventoryService.returnToStock(serviceOrder, updated, release,
                            "Devolução parcial por redução de quantidade aplicada");
                } else {
                    inventoryService.cancelReservation(serviceOrder, updated, release,
                            "Cancelamento parcial de reserva por redução de quantidade");
                }
            }
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
