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

    public ServiceOrderItemResponseDTO addItem(Long serviceOrderId, ServiceOrderItemCreateDTO dto) {
        ServiceOrder serviceOrder = serviceOrderRepository.findById(serviceOrderId)
            .orElseThrow(() -> new RuntimeException("Ordem de serviço não encontrada"));
        logger.info("Service Order: {}", serviceOrder.toString());
        logger.info("Service Order Item: {}", dto);
        logger.info("Service Order ID: {}", serviceOrderId);

        ServiceOrderItem item = new ServiceOrderItem();
        item.setServiceOrder(serviceOrder);
        ServiceOrderService.getServiceOrderItem(dto, item);

        ServiceOrderItem saved = itemRepository.save(item);
        logger.info("Service Order Item Saved: {}", saved.toString());
        logger.info("Service Order To Be Saved: {}", serviceOrder.toString());

        if (Boolean.TRUE.equals(saved.getRequiresStock()) && saved.getStockProductId() != null) {
            inventoryService.reservePart(saved.getStockProductId(), saved.getQuantity(), saved,
                    "Reserva manual ao adicionar item");
            saved.setStockReserved(true);
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
        Long previousStockProductId = item.getStockProductId();
        int previousQuantity = item.getQuantity();
        boolean previousReserved = Boolean.TRUE.equals(item.getStockReserved());

        if (dto.getDescription() != null) {
            item.setDescription(dto.getDescription());
        }
        if (dto.getItemType() != null) {
            item.setItemType(dto.getItemType());
        }
        if (dto.getQuantity() != null) {
            item.setQuantity(dto.getQuantity());
        }
        if (dto.getUnitPrice() != null) {
            item.setUnitPrice(dto.getUnitPrice());
        }
        if (dto.getProductCode() != null) {
            item.setProductCode(dto.getProductCode());
        }
        if (dto.getStockProductId() != null) {
            item.setStockProductId(dto.getStockProductId());
        }
        if (dto.getRequiresStock() != null) {
            item.setRequiresStock(dto.getRequiresStock());
        }
        if (dto.getObservations() != null) {
            item.setObservations(dto.getObservations());
        }

        ServiceOrderItem updated = itemRepository.save(item);

        handleInventoryUpdate(previousRequiresStock, previousStockProductId, previousQuantity, previousReserved, updated);

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
        itemRepository.deleteById(id);
        
        // Recalcular custos da OS
        serviceOrder.calculateTotalCost();
        serviceOrderRepository.save(serviceOrder);
    }

    public ServiceOrderItemResponseDTO applyItem(Long id) {
        ServiceOrderItem item = itemRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Item não encontrado"));

        if (Boolean.TRUE.equals(item.getRequiresStock()) && item.getStockProductId() != null
                && Boolean.TRUE.equals(item.getStockReserved())) {
            inventoryService.consumeReservedPart(item.getStockProductId(), item.getQuantity(), item,
                    "Baixa de estoque ao aplicar item");
            item.setStockReserved(false);
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

        if (Boolean.TRUE.equals(item.getRequiresStock()) && item.getStockProductId() != null
                && Boolean.TRUE.equals(item.getApplied())) {
            inventoryService.returnPart(item.getStockProductId(), item.getQuantity(), true, item,
                    "Devolução ao estoque ao desaplicar item");
            item.setStockReserved(true);
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
        
        if (Boolean.TRUE.equals(item.getRequiresStock()) && item.getStockProductId() != null
                && !Boolean.TRUE.equals(item.getStockReserved())) {
            inventoryService.reservePart(item.getStockProductId(), item.getQuantity(), item,
                    "Reserva manual de estoque");
            item.setStockReserved(true);
        }
        ServiceOrderItem updated = itemRepository.save(item);
        return convertToResponseDTO(updated);
    }

    public ServiceOrderItemResponseDTO releaseStock(Long id) {
        ServiceOrderItem item = itemRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Item não encontrado"));

        if (Boolean.TRUE.equals(item.getRequiresStock()) && item.getStockProductId() != null
                && Boolean.TRUE.equals(item.getStockReserved())) {
            inventoryService.releaseReservation(item.getStockProductId(), item.getQuantity(), item,
                    "Liberação manual de estoque");
            item.setStockReserved(false);
        }
        ServiceOrderItem updated = itemRepository.save(item);
        return convertToResponseDTO(updated);
    }

    private void handleInventoryUpdate(boolean previousRequiresStock,
                                       Long previousStockProductId,
                                       int previousQuantity,
                                       boolean previousReserved,
                                       ServiceOrderItem updated) {
        boolean currentRequiresStock = Boolean.TRUE.equals(updated.getRequiresStock());
        Long currentStockProductId = updated.getStockProductId();
        int currentQuantity = updated.getQuantity();

        if (!previousRequiresStock && currentRequiresStock && currentStockProductId != null) {
            inventoryService.reservePart(currentStockProductId, currentQuantity, updated,
                    "Reserva ao atualizar item");
            updated.setStockReserved(true);
            return;
        }

        if (previousRequiresStock && !currentRequiresStock && previousStockProductId != null && previousReserved) {
            inventoryService.releaseReservation(previousStockProductId, previousQuantity, updated,
                    "Liberação de reserva ao remover controle de estoque");
            updated.setStockReserved(false);
            return;
        }

        if (previousRequiresStock && currentRequiresStock) {
            if (!java.util.Objects.equals(previousStockProductId, currentStockProductId)) {
                if (previousStockProductId != null && previousReserved) {
                    inventoryService.releaseReservation(previousStockProductId, previousQuantity, updated,
                            "Liberação de reserva ao alterar peça vinculada");
                }
                if (currentStockProductId != null) {
                    inventoryService.reservePart(currentStockProductId, currentQuantity, updated,
                            "Reserva ao alterar peça vinculada");
                    updated.setStockReserved(true);
                }
                return;
            }

            if (currentStockProductId != null && previousReserved) {
                int difference = currentQuantity - previousQuantity;
                if (difference > 0) {
                    inventoryService.reservePart(currentStockProductId, difference, updated,
                            "Reserva adicional ao atualizar quantidade");
                } else if (difference < 0) {
                    inventoryService.releaseReservation(currentStockProductId, Math.abs(difference), updated,
                            "Liberação parcial ao atualizar quantidade");
                }
            }
        }
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
        dto.setStockProductId(item.getStockProductId());
        dto.setRequiresStock(item.getRequiresStock());
        dto.setStockReserved(item.getStockReserved());
        dto.setApplied(item.getApplied());
        dto.setObservations(item.getObservations());
        dto.setCreatedAt(item.getCreatedAt());
        dto.setUpdatedAt(item.getUpdatedAt());
        return dto;
    }
}
