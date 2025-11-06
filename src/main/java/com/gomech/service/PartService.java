package com.gomech.service;

import com.gomech.domain.InventoryItem;
import com.gomech.domain.Part;
import com.gomech.dto.Inventory.InventoryEntryRequestDTO;
import com.gomech.dto.Parts.PartCreateDTO;
import com.gomech.dto.Parts.PartMapper;
import com.gomech.dto.Parts.PartResponseDTO;
import com.gomech.dto.Parts.PartUpdateDTO;
import com.gomech.dto.ServiceOrder.ServiceOrderItemCreateDTO;
import com.gomech.dto.ServiceOrder.ServiceOrderItemResponseDTO;
import com.gomech.model.ServiceOrderItemType;
import com.gomech.repository.InventoryItemRepository;
import com.gomech.repository.PartRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static java.util.Objects.requireNonNullElse;

@Service
@Transactional
public class PartService {

    private final PartRepository partRepository;
    private final InventoryService inventoryService;
    private final ServiceOrderItemService serviceOrderItemService;
    private final InventoryItemRepository inventoryItemRepository;

    public PartService(PartRepository partRepository,
                       InventoryService inventoryService,
                       ServiceOrderItemService serviceOrderItemService,
                       InventoryItemRepository inventoryItemRepository) {
        this.partRepository = partRepository;
        this.inventoryService = inventoryService;
        this.serviceOrderItemService = serviceOrderItemService;
        this.inventoryItemRepository = inventoryItemRepository;
    }

    public PartResponseDTO register(PartCreateDTO dto) {
        Part part = PartMapper.toEntity(dto);
        Part saved = partRepository.save(part);

        registerInitialStockIfRequested(saved, dto);
        linkPartToServiceOrderIfRequested(saved, dto);

        return PartResponseDTO.fromEntity(saved);
    }

    public PartResponseDTO update(Long id, PartUpdateDTO updates) {
        Part existing = partRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Peça não encontrada"));

        PartMapper.updateEntity(existing, updates);

        return PartResponseDTO.fromEntity(partRepository.save(existing));
    }

    @Transactional(readOnly = true)
    public Optional<PartResponseDTO> getById(Long id) {
        return partRepository.findById(id)
                .map(PartResponseDTO::fromEntity);
    }

    @Transactional(readOnly = true)
    public List<PartResponseDTO> listAll() {
        return partRepository.findAll().stream()
                .map(PartResponseDTO::fromEntity)
                .toList();
    }

    public void delete(Long id) {
        if (!partRepository.existsById(id)) {
            throw new IllegalArgumentException("Peça não encontrada");
        }
        partRepository.deleteById(id);
    }

    private void registerInitialStockIfRequested(Part part, PartCreateDTO dto) {
        if (dto.stockLocation() == null || dto.stockQuantity() == null) {
            return;
        }

        InventoryEntryRequestDTO request = new InventoryEntryRequestDTO(
                part.getId(),
                dto.stockLocation(),
                dto.stockQuantity(),
                requireNonNullElse(dto.stockUnitCost(), dto.unitCost()),
                requireNonNullElse(dto.stockSalePrice(), dto.unitPrice()),
                part.getSku(),
                "Cadastro de peça direcionado ao estoque"
        );

        inventoryService.registerEntry(request);
    }

    private void linkPartToServiceOrderIfRequested(Part part, PartCreateDTO dto) {
        if (dto.serviceOrderId() == null) {
            return;
        }

        ServiceOrderItemCreateDTO itemDto = new ServiceOrderItemCreateDTO();
        itemDto.setPartId(part.getId());
        itemDto.setItemType(ServiceOrderItemType.PART);
        itemDto.setRequiresStock(true);
        itemDto.setDescription(part.getName());
        itemDto.setProductCode(part.getSku());

        if (dto.serviceQuantity() != null) {
            itemDto.setQuantity(dto.serviceQuantity());
        }

        BigDecimal unitPrice = dto.serviceUnitPrice() != null
                ? dto.serviceUnitPrice()
                : requireNonNullElse(part.getUnitPrice(), dto.unitPrice());
        if (unitPrice != null) {
            itemDto.setUnitPrice(unitPrice);
        }

        Long inventoryItemId = resolveInventoryItemId(part, dto);
        if (inventoryItemId == null) {
            throw new IllegalStateException("Não foi possível localizar item de estoque para vincular à ordem de serviço");
        }
        itemDto.setInventoryItemId(inventoryItemId);

        ServiceOrderItemResponseDTO itemResponse = serviceOrderItemService.addItem(dto.serviceOrderId(), itemDto);
        serviceOrderItemService.applyItem(itemResponse.getId());
    }

    private Long resolveInventoryItemId(Part part, PartCreateDTO dto) {
        if (dto.inventoryItemId() != null) {
            return dto.inventoryItemId();
        }

        if (dto.stockLocation() != null) {
            Optional<InventoryItem> byLocation = inventoryItemRepository.findByPartIdAndLocation(part.getId(), dto.stockLocation());
            if (byLocation.isPresent()) {
                return byLocation.get().getId();
            }
        }

        return inventoryItemRepository.findByPartId(part.getId()).stream()
                .findFirst()
                .map(InventoryItem::getId)
                .orElse(null);
    }
}
