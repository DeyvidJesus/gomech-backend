package com.gomech.service;

import com.gomech.domain.InventoryItem;
import com.gomech.domain.InventoryMovement;
import com.gomech.domain.InventoryMovementType;
import com.gomech.domain.Part;
import com.gomech.dto.Inventory.InventoryEntryRequestDTO;
import com.gomech.dto.Inventory.InventoryItemCreateDTO;
import com.gomech.dto.Inventory.InventoryItemResponseDTO;
import com.gomech.dto.Inventory.InventoryItemUpdateDTO;
import com.gomech.dto.Inventory.InventoryMovementResponseDTO;
import com.gomech.dto.Inventory.StockCancellationRequestDTO;
import com.gomech.dto.Inventory.StockConsumptionRequestDTO;
import com.gomech.dto.Inventory.StockReservationRequestDTO;
import com.gomech.dto.Inventory.StockReturnRequestDTO;
import com.gomech.model.ServiceOrder;
import com.gomech.model.ServiceOrderItem;
import com.gomech.repository.InventoryItemRepository;
import com.gomech.repository.InventoryMovementRepository;
import com.gomech.repository.PartRepository;
import com.gomech.repository.ServiceOrderItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

@Service
@Transactional
public class InventoryService {

    private final InventoryItemRepository inventoryItemRepository;
    private final InventoryMovementRepository inventoryMovementRepository;
    private final PartRepository partRepository;
    private final ServiceOrderItemRepository serviceOrderItemRepository;
    private final InventoryAlertService inventoryAlertService;
    private final AuditService auditService;

    public InventoryService(InventoryItemRepository inventoryItemRepository,
                            InventoryMovementRepository inventoryMovementRepository,
                            PartRepository partRepository,
                            ServiceOrderItemRepository serviceOrderItemRepository,
                            InventoryAlertService inventoryAlertService,
                            AuditService auditService) {
        this.inventoryItemRepository = inventoryItemRepository;
        this.inventoryMovementRepository = inventoryMovementRepository;
        this.partRepository = partRepository;
        this.serviceOrderItemRepository = serviceOrderItemRepository;
        this.inventoryAlertService = inventoryAlertService;
        this.auditService = auditService;
    }

    public InventoryItemResponseDTO createItem(InventoryItemCreateDTO dto) {
        Part part = partRepository.findById(dto.partId())
                .orElseThrow(() -> new IllegalArgumentException("Peça não encontrada"));

        inventoryItemRepository.findByPartIdAndLocation(dto.partId(), dto.location())
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("Já existe um item de estoque para esta peça e localização");
                });

        InventoryItem item = new InventoryItem();
        item.setPart(part);
        item.setLocation(dto.location());
        item.setQuantity(0);
        item.setReservedQuantity(0);
        item.setMinimumQuantity(dto.minimumQuantity() != null ? dto.minimumQuantity() : 0);
        item.setUnitCost(dto.unitCost());
        item.setSalePrice(dto.salePrice());

        InventoryItem saved = inventoryItemRepository.save(item);
        auditService.logEntityAction("CREATE", "INVENTORY_ITEM", saved.getId(),
                "Item criado para peça " + part.getId());
        inventoryAlertService.onStockLevelChanged(saved);
        return InventoryItemResponseDTO.fromEntity(saved);
    }

    public InventoryItemResponseDTO updateItem(Long id, InventoryItemUpdateDTO dto) {
        InventoryItem item = findInventoryItemById(id);

        if (dto.location() != null) {
            item.setLocation(dto.location());
        }
        if (dto.minimumQuantity() != null) {
            item.setMinimumQuantity(dto.minimumQuantity());
        }
        if (dto.unitCost() != null) {
            item.setUnitCost(dto.unitCost());
        }
        if (dto.salePrice() != null) {
            item.setSalePrice(dto.salePrice());
        }

        InventoryItem saved = inventoryItemRepository.save(item);
        auditService.logEntityAction("UPDATE", "INVENTORY_ITEM", saved.getId(),
                "Item atualizado para peça " + saved.getPart().getId());
        return InventoryItemResponseDTO.fromEntity(saved);
    }

    public void deleteItem(Long id) {
        InventoryItem item = findInventoryItemById(id);
        inventoryItemRepository.deleteById(id);
        auditService.logEntityAction("DELETE", "INVENTORY_ITEM", id,
                "Item removido para peça " + item.getPart().getId());
    }

    @Transactional(readOnly = true)
    public List<InventoryItemResponseDTO> listItems(Long partId) {
        List<InventoryItem> items = partId != null
                ? inventoryItemRepository.findByPartId(partId)
                : inventoryItemRepository.findAll();

        return items.stream()
                .map(InventoryItemResponseDTO::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public InventoryItemResponseDTO getItem(Long id) {
        return inventoryItemRepository.findById(id)
                .map(InventoryItemResponseDTO::fromEntity)
                .orElseThrow(() -> new IllegalArgumentException("Item de estoque não encontrado"));
    }

    public InventoryMovementResponseDTO registerEntry(InventoryEntryRequestDTO dto) {
        InventoryMovement movement = registerEntry(
                dto.partId(),
                dto.location(),
                dto.quantity(),
                dto.unitCost(),
                dto.salePrice(),
                dto.referenceCode(),
                dto.notes()
        );
        return InventoryMovementResponseDTO.fromEntity(movement);
    }

    public InventoryMovementResponseDTO reserveStock(StockReservationRequestDTO dto) {
        ServiceOrderItem item = findServiceOrderItem(dto.serviceOrderItemId());
        InventoryMovement movement = reserveStock(item.getServiceOrder(), item, dto.quantity(), dto.notes());
        return InventoryMovementResponseDTO.fromEntity(movement);
    }

    public InventoryMovementResponseDTO consumeStock(StockConsumptionRequestDTO dto) {
        ServiceOrderItem item = findServiceOrderItem(dto.serviceOrderItemId());
        InventoryMovement movement = consumeStock(item.getServiceOrder(), item, dto.quantity(), dto.notes());
        return InventoryMovementResponseDTO.fromEntity(movement);
    }

    public InventoryMovementResponseDTO cancelReservation(StockCancellationRequestDTO dto) {
        ServiceOrderItem item = findServiceOrderItem(dto.serviceOrderItemId());
        InventoryMovement movement = cancelReservation(item.getServiceOrder(), item, dto.quantity(), dto.notes());
        return InventoryMovementResponseDTO.fromEntity(movement);
    }

    public InventoryMovementResponseDTO returnToStock(StockReturnRequestDTO dto) {
        ServiceOrderItem item = findServiceOrderItem(dto.serviceOrderItemId());
        InventoryMovement movement = returnToStock(item.getServiceOrder(), item, dto.quantity(), dto.notes());
        return InventoryMovementResponseDTO.fromEntity(movement);
    }

    @Transactional(readOnly = true)
    public List<InventoryMovementResponseDTO> listMovements(Long inventoryItemId, Long serviceOrderId, Long vehicleId) {
        List<InventoryMovement> movements;
        if (inventoryItemId != null) {
            movements = inventoryMovementRepository.findByInventoryItemId(inventoryItemId);
        } else if (serviceOrderId != null) {
            movements = inventoryMovementRepository.findByServiceOrderId(serviceOrderId);
        } else if (vehicleId != null) {
            movements = inventoryMovementRepository.findByVehicleId(vehicleId);
        } else {
            movements = inventoryMovementRepository.findAll();
        }

        return movements.stream()
                .map(InventoryMovementResponseDTO::fromEntity)
                .toList();
    }

    public InventoryMovement registerEntry(Long partId,
                                           String location,
                                           int quantity,
                                           BigDecimal unitCost,
                                           BigDecimal salePrice,
                                           String referenceCode,
                                           String notes) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantidade de entrada deve ser maior que zero");
        }

        Part part = partRepository.findById(partId)
                .orElseThrow(() -> new IllegalArgumentException("Peça não encontrada"));

        InventoryItem item = inventoryItemRepository.findByPartIdAndLocation(partId, location)
                .orElseGet(() -> createInventoryItem(part, location));

        item.setQuantity(item.getQuantity() + quantity);
        if (unitCost != null) {
            item.setUnitCost(unitCost);
        }
        if (salePrice != null) {
            item.setSalePrice(salePrice);
        }
        InventoryItem savedItem = inventoryItemRepository.save(item);

        InventoryMovement movement = recordMovement(savedItem, part, null, null, InventoryMovementType.IN, quantity, referenceCode, notes);
        inventoryAlertService.onStockLevelChanged(savedItem);
        auditService.logEntityAction(movement.getMovementType().name(), "INVENTORY_MOVEMENT", movement.getId(),
                String.format("itemId=%d;quantity=%d;type=ENTRY", savedItem.getId(), quantity));
        return movement;
    }

    public InventoryMovement reserveStock(ServiceOrder serviceOrder,
                                          ServiceOrderItem serviceOrderItem,
                                          int quantity,
                                          String notes) {
        validateServiceOrderItem(serviceOrderItem);
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantidade de reserva deve ser maior que zero");
        }

        InventoryItem inventoryItem = getInventoryItem(serviceOrderItem);
        ensureAvailableStock(inventoryItem, quantity);

        inventoryItem.setReservedQuantity(inventoryItem.getReservedQuantity() + quantity);
        InventoryItem savedItem = inventoryItemRepository.save(inventoryItem);

        serviceOrderItem.setStockReserved(true);
        serviceOrderItem.setInventoryItem(savedItem);
        serviceOrderItemRepository.save(serviceOrderItem);

        InventoryMovement movement = recordMovement(savedItem, savedItem.getPart(), serviceOrder, serviceOrderItem,
                InventoryMovementType.ADJUSTMENT, quantity, serviceOrder.getOrderNumber(),
                defaultNotes(notes, "Reserva de estoque"));
        inventoryAlertService.onStockLevelChanged(savedItem);
        auditService.logEntityAction(movement.getMovementType().name(), "INVENTORY_MOVEMENT", movement.getId(),
                String.format("itemId=%d;quantity=%d;type=RESERVATION", savedItem.getId(), quantity));
        return movement;
    }

    public InventoryMovement consumeStock(ServiceOrder serviceOrder,
                                          ServiceOrderItem serviceOrderItem,
                                          int quantity,
                                          String notes) {
        validateServiceOrderItem(serviceOrderItem);
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantidade de baixa deve ser maior que zero");
        }

        InventoryItem inventoryItem = getInventoryItem(serviceOrderItem);
        if (inventoryItem.getReservedQuantity() < quantity) {
            throw new IllegalStateException("Quantidade reservada insuficiente para baixa");
        }
        if (inventoryItem.getQuantity() < quantity) {
            throw new IllegalStateException("Estoque insuficiente para baixa");
        }

        inventoryItem.setReservedQuantity(inventoryItem.getReservedQuantity() - quantity);
        return performConsumption(serviceOrder, serviceOrderItem, inventoryItem, quantity,
                defaultNotes(notes, "Baixa de estoque"));
    }

    public InventoryMovement consumeDirect(ServiceOrder serviceOrder,
                                           ServiceOrderItem serviceOrderItem,
                                           int quantity,
                                           String notes) {
        validateServiceOrderItem(serviceOrderItem);
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantidade de baixa deve ser maior que zero");
        }

        InventoryItem inventoryItem = getInventoryItem(serviceOrderItem);
        ensureAvailableStock(inventoryItem, quantity);

        return performConsumption(serviceOrder, serviceOrderItem, inventoryItem, quantity,
                defaultNotes(notes, "Baixa direta de estoque"));
    }

    public InventoryMovement cancelReservation(ServiceOrder serviceOrder,
                                                ServiceOrderItem serviceOrderItem,
                                                int quantity,
                                                String notes) {
        validateServiceOrderItem(serviceOrderItem);
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantidade para cancelamento deve ser maior que zero");
        }

        InventoryItem inventoryItem = getInventoryItem(serviceOrderItem);
        if (inventoryItem.getReservedQuantity() < quantity) {
            throw new IllegalStateException("Quantidade reservada insuficiente para cancelamento");
        }

        inventoryItem.setReservedQuantity(inventoryItem.getReservedQuantity() - quantity);
        InventoryItem savedItem = inventoryItemRepository.save(inventoryItem);

        serviceOrderItem.setStockReserved(false);
        serviceOrderItem.setInventoryItem(savedItem);
        serviceOrderItemRepository.save(serviceOrderItem);

        InventoryMovement movement = recordMovement(savedItem, savedItem.getPart(), serviceOrder, serviceOrderItem,
                InventoryMovementType.ADJUSTMENT, quantity, serviceOrder.getOrderNumber(),
                defaultNotes(notes, "Cancelamento de reserva"));
        inventoryAlertService.onStockLevelChanged(savedItem);
        auditService.logEntityAction(movement.getMovementType().name(), "INVENTORY_MOVEMENT", movement.getId(),
                String.format("itemId=%d;quantity=%d;type=CANCELLATION", savedItem.getId(), quantity));
        return movement;
    }

    public InventoryMovement returnToStock(ServiceOrder serviceOrder,
                                           ServiceOrderItem serviceOrderItem,
                                           int quantity,
                                           String notes) {
        validateServiceOrderItem(serviceOrderItem);
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantidade de devolução deve ser maior que zero");
        }

        InventoryItem inventoryItem = getInventoryItem(serviceOrderItem);
        inventoryItem.setQuantity(inventoryItem.getQuantity() + quantity);
        InventoryItem savedItem = inventoryItemRepository.save(inventoryItem);

        serviceOrderItem.setStockReserved(false);
        serviceOrderItem.setInventoryItem(savedItem);
        serviceOrderItemRepository.save(serviceOrderItem);

        InventoryMovement movement = recordMovement(savedItem, savedItem.getPart(), serviceOrder, serviceOrderItem,
                InventoryMovementType.IN, quantity, serviceOrder.getOrderNumber(),
                defaultNotes(notes, "Devolução ao estoque"));
        inventoryAlertService.onStockLevelChanged(savedItem);
        auditService.logEntityAction(movement.getMovementType().name(), "INVENTORY_MOVEMENT", movement.getId(),
                String.format("itemId=%d;quantity=%d;type=RETURN", savedItem.getId(), quantity));
        return movement;
    }

    public void reconcileServiceOrderInventory(ServiceOrder serviceOrder) {
        for (ServiceOrderItem item : serviceOrder.getItems()) {
            if (!Boolean.TRUE.equals(item.getRequiresStock())) {
                continue;
            }

            if (Boolean.TRUE.equals(item.getApplied())) {
                returnToStock(serviceOrder, item, item.getQuantity(), "Conciliação de estoque - devolução");
                item.unapply();
                serviceOrderItemRepository.save(item);
                continue;
            }

            if (Boolean.TRUE.equals(item.getStockReserved())) {
                cancelReservation(serviceOrder, item, item.getQuantity(),
                        "Conciliação de estoque - liberação");
            }
        }
    }

    public void reserveItemsForOrder(ServiceOrder serviceOrder) {
        for (ServiceOrderItem item : serviceOrder.getItems()) {
            if (Boolean.TRUE.equals(item.getRequiresStock()) && !Boolean.TRUE.equals(item.getStockReserved())) {
                reserveStock(serviceOrder, item, item.getQuantity(), "Reserva automática de itens da OS");
            }
        }
    }

    private void validateServiceOrderItem(ServiceOrderItem item) {
        if (!Boolean.TRUE.equals(item.getRequiresStock())) {
            throw new IllegalArgumentException("Item não requer controle de estoque");
        }
        if (item.getInventoryItem() == null || item.getInventoryItem().getId() == null) {
            throw new IllegalArgumentException("Item não possui referência de estoque");
        }
    }

    private InventoryItem getInventoryItem(ServiceOrderItem serviceOrderItem) {
        InventoryItem inventoryItem = serviceOrderItem.getInventoryItem();
        if (inventoryItem == null || inventoryItem.getId() == null) {
            throw new IllegalArgumentException("Item de estoque não encontrado");
        }
        return inventoryItemRepository.findById(inventoryItem.getId())
                .orElseThrow(() -> new IllegalArgumentException("Item de estoque não encontrado"));
    }

    private void ensureAvailableStock(InventoryItem inventoryItem, int quantity) {
        int available = inventoryItem.getQuantity() - inventoryItem.getReservedQuantity();
        if (available < quantity) {
            throw new IllegalStateException("Estoque insuficiente para reserva");
        }
    }

    private InventoryItem createInventoryItem(Part part, String location) {
        InventoryItem item = new InventoryItem();
        item.setPart(part);
        item.setLocation(location);
        item.setQuantity(0);
        item.setReservedQuantity(0);
        item.setMinimumQuantity(0);
        return inventoryItemRepository.save(item);
    }

    private InventoryMovement recordMovement(InventoryItem inventoryItem,
                                             Part part,
                                             ServiceOrder serviceOrder,
                                             ServiceOrderItem serviceOrderItem,
                                             InventoryMovementType type,
                                             int quantity,
                                             String referenceCode,
                                             String notes) {
        InventoryMovement movement = new InventoryMovement();
        movement.setInventoryItem(inventoryItem);
        movement.setPart(part);
        movement.setServiceOrder(serviceOrder);
        if (serviceOrder != null) {
            movement.setVehicle(serviceOrder.getVehicle());
        }
        movement.setMovementType(type);
        movement.setQuantity(quantity);
        movement.setReferenceCode(referenceCode);
        movement.setNotes(notes);
        InventoryMovement savedMovement = inventoryMovementRepository.save(movement);

        inventoryItem.addMovement(savedMovement);
        inventoryItemRepository.save(inventoryItem);

        if (serviceOrder != null) {
            serviceOrder.addInventoryMovement(savedMovement);
        }

        return savedMovement;
    }

    private String defaultNotes(String provided, String fallback) {
        return Objects.requireNonNullElse(provided, fallback);
    }

    private InventoryMovement performConsumption(ServiceOrder serviceOrder,
                                                 ServiceOrderItem serviceOrderItem,
                                                 InventoryItem inventoryItem,
                                                 int quantity,
                                                 String notes) {
        inventoryItem.setQuantity(inventoryItem.getQuantity() - quantity);
        InventoryItem savedItem = inventoryItemRepository.save(inventoryItem);

        serviceOrderItem.setStockReserved(false);
        serviceOrderItem.setInventoryItem(savedItem);
        serviceOrderItemRepository.save(serviceOrderItem);

        InventoryMovement movement = recordMovement(savedItem, savedItem.getPart(), serviceOrder, serviceOrderItem,
                InventoryMovementType.OUT, quantity,
                serviceOrder != null ? serviceOrder.getOrderNumber() : null,
                notes);
        inventoryAlertService.onStockLevelChanged(savedItem);
        return movement;
    }

    private InventoryItem findInventoryItemById(Long id) {
        return inventoryItemRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Item de estoque não encontrado"));
    }

    private ServiceOrderItem findServiceOrderItem(Long id) {
        ServiceOrderItem item = serviceOrderItemRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Item da ordem de serviço não encontrado"));
        if (!Boolean.TRUE.equals(item.getRequiresStock())) {
            throw new IllegalArgumentException("Item informado não requer controle de estoque");
        }
        return item;
    }
}
