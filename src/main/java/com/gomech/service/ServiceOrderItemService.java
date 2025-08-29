package com.gomech.service;

import com.gomech.dto.ServiceOrderItemCreateDTO;
import com.gomech.dto.ServiceOrderItemResponseDTO;
import com.gomech.model.ServiceOrder;
import com.gomech.model.ServiceOrderItem;
import com.gomech.model.ServiceOrderItemType;
import com.gomech.repository.ServiceOrderItemRepository;
import com.gomech.repository.ServiceOrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class ServiceOrderItemService {
    
    @Autowired
    private ServiceOrderItemRepository itemRepository;
    
    @Autowired
    private ServiceOrderRepository serviceOrderRepository;

    public ServiceOrderItemResponseDTO addItem(Long serviceOrderId, ServiceOrderItemCreateDTO dto) {
        ServiceOrder serviceOrder = serviceOrderRepository.findById(serviceOrderId)
            .orElseThrow(() -> new RuntimeException("Ordem de serviço não encontrada"));

        ServiceOrderItem item = new ServiceOrderItem();
        item.setServiceOrder(serviceOrder);
        item.setDescription(dto.getDescription());
        item.setItemType(dto.getItemType());
        item.setQuantity(dto.getQuantity());
        item.setUnitPrice(dto.getUnitPrice());
        item.setProductCode(dto.getProductCode());
        item.setRequiresStock(dto.getRequiresStock());
        item.setObservations(dto.getObservations());

        ServiceOrderItem saved = itemRepository.save(item);
        
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
        if (dto.getRequiresStock() != null) {
            item.setRequiresStock(dto.getRequiresStock());
        }
        if (dto.getObservations() != null) {
            item.setObservations(dto.getObservations());
        }

        ServiceOrderItem updated = itemRepository.save(item);
        
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
        
        item.apply();
        ServiceOrderItem updated = itemRepository.save(item);
        return convertToResponseDTO(updated);
    }

    public ServiceOrderItemResponseDTO unapplyItem(Long id) {
        ServiceOrderItem item = itemRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Item não encontrado"));
        
        item.unapply();
        ServiceOrderItem updated = itemRepository.save(item);
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
        
        // TODO: Implementar lógica de reserva de estoque quando o módulo de estoque for criado
        item.setStockReserved(true);
        ServiceOrderItem updated = itemRepository.save(item);
        return convertToResponseDTO(updated);
    }

    public ServiceOrderItemResponseDTO releaseStock(Long id) {
        ServiceOrderItem item = itemRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Item não encontrado"));
        
        // TODO: Implementar lógica de liberação de estoque quando o módulo de estoque for criado
        item.setStockReserved(false);
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
        dto.setRequiresStock(item.getRequiresStock());
        dto.setStockReserved(item.getStockReserved());
        dto.setApplied(item.getApplied());
        dto.setObservations(item.getObservations());
        dto.setCreatedAt(item.getCreatedAt());
        dto.setUpdatedAt(item.getUpdatedAt());
        return dto;
    }
}
