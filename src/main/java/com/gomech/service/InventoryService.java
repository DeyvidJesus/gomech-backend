package com.gomech.service;

import com.gomech.domain.InventoryItem;
import com.gomech.domain.InventoryMovement;
import com.gomech.domain.InventoryMovementType;
import com.gomech.domain.Part;
import com.gomech.context.OrganizationContext;
import com.gomech.model.Organization;
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
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.poi.ss.usermodel.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
        item.setOrganization(resolveOrganizationFromPart(part));
        item.setLocation(dto.location());
        item.setQuantity(0);
        item.setReservedQuantity(0);
        item.setMinimumQuantity(0);
        item.setUnitCost(dto.unitCost());
        item.setSalePrice(dto.salePrice());

        InventoryItem saved = inventoryItemRepository.save(item);
        auditService.logEntityAction("CREATE", "INVENTORY_ITEM", saved.getId(),
                "Item criado para peça " + part.getId());
        
        // Registra quantidade inicial se fornecida
        if (dto.initialQuantity() != null && dto.initialQuantity() > 0) {
            registerEntry(
                    dto.partId(),
                    dto.location(),
                    dto.initialQuantity(),
                    dto.unitCost(),
                    dto.salePrice(),
                    "INITIAL",
                    "Quantidade inicial ao criar item de estoque"
            );
            saved = inventoryItemRepository.findById(saved.getId())
                    .orElse(saved);
        }
        
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
    public Page<InventoryItemResponseDTO> listItemsPaginated(Pageable pageable, Long partId) {
        Page<InventoryItem> items = partId != null
                ? inventoryItemRepository.findByPartId(partId, pageable)
                : inventoryItemRepository.findAll(pageable);

        return items.map(InventoryItemResponseDTO::fromEntity);
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
        
        // Se não tem inventoryItem mas tem peça, tenta buscar automaticamente
        if ((item.getInventoryItem() == null || item.getInventoryItem().getId() == null) && item.getPart() != null) {
            InventoryItem autoInventoryItem = inventoryItemRepository.findByPartId(item.getPart().getId()).stream()
                    .findFirst()
                    .orElse(null);
            if (autoInventoryItem != null) {
                item.setInventoryItem(autoInventoryItem);
                serviceOrderItemRepository.save(item);
            }
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
        item.setOrganization(resolveOrganizationFromPart(part));
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
        if (inventoryItem.getOrganization() != null) {
            movement.setOrganization(inventoryItem.getOrganization());
        } else {
            movement.setOrganization(requireOrganization());
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
        
        // Se não tem inventoryItem mas tem peça, tenta buscar automaticamente
        if ((item.getInventoryItem() == null || item.getInventoryItem().getId() == null) && item.getPart() != null) {
            InventoryItem autoInventoryItem = inventoryItemRepository.findByPartId(item.getPart().getId()).stream()
                    .findFirst()
                    .orElse(null);
            if (autoInventoryItem != null) {
                item.setInventoryItem(autoInventoryItem);
                serviceOrderItemRepository.save(item);
            }
        }
        
        return item;
    }

    private Organization resolveOrganizationFromPart(Part part) {
        if (part.getOrganization() != null) {
            return part.getOrganization();
        }
        return requireOrganization();
    }

    private Organization requireOrganization() {
        Organization organization = OrganizationContext.getOrganization();
        if (organization == null) {
            throw new IllegalStateException("Organização não encontrada no contexto da requisição");
        }
        return organization;
    }

    public List<InventoryItemResponseDTO> saveFromFile(MultipartFile file) {
        List<InventoryItem> items;
        String filename = file.getOriginalFilename();
        if (filename == null) throw new RuntimeException("Arquivo sem nome");

        if (filename.endsWith(".csv") || filename.endsWith(".txt")) {
            items = saveFromDelimitedFile(file);
        } else if (filename.endsWith(".xls") || filename.endsWith(".xlsx")) {
            items = saveFromExcel(file);
        } else {
            throw new RuntimeException("Formato de arquivo não suportado. Use CSV ou XLSX");
        }

        return items.stream()
                .map(InventoryItemResponseDTO::fromEntity)
                .toList();
    }

    private List<InventoryItem> saveFromDelimitedFile(MultipartFile file) {
        List<InventoryItem> items = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line = reader.readLine(); // Ignora cabeçalho
            
            while ((line = reader.readLine()) != null) {
                String[] fields = line.split(",");
                if (fields.length < 3) continue; // Precisa de partId, location, quantity
                
                Long partId = Long.parseLong(fields[0].trim());
                Part part = partRepository.findById(partId)
                        .orElseThrow(() -> new RuntimeException("Peça não encontrada: " + partId));
                
                InventoryItem item = new InventoryItem();
                item.setPart(part);
                item.setLocation(fields[1].trim());
                item.setQuantity(Integer.parseInt(fields[2].trim()));
                if (fields.length > 3) item.setReservedQuantity(Integer.parseInt(fields[3].trim()));
                if (fields.length > 4) item.setMinimumQuantity(Integer.parseInt(fields[4].trim()));
                if (fields.length > 5) item.setUnitCost(new BigDecimal(fields[5].trim()));
                if (fields.length > 6) item.setSalePrice(new BigDecimal(fields[6].trim()));
                items.add(item);
            }
        } catch (IOException e) {
            throw new RuntimeException("Falha ao processar arquivo CSV", e);
        }
        
        List<InventoryItem> saved = inventoryItemRepository.saveAll(items);
        saved.forEach(item -> {
            auditService.logEntityAction("CREATE", "INVENTORY_ITEM", item.getId(),
                    "Item cadastrado via arquivo");
            inventoryAlertService.onStockLevelChanged(item);
        });
        return saved;
    }

    private List<InventoryItem> saveFromExcel(MultipartFile file) {
        List<InventoryItem> items = new ArrayList<>();
        try (InputStream is = file.getInputStream(); Workbook workbook = WorkbookFactory.create(is)) {
            DataFormatter formatter = new DataFormatter();
            Sheet sheet = workbook.getSheetAt(0);
            Map<String, Integer> headers = new HashMap<>();
            boolean first = true;
            
            for (Row row : sheet) {
                if (row == null) continue;
                
                if (first) {
                    for (Cell cell : row) {
                        headers.put(formatter.formatCellValue(cell).toLowerCase().replace("_", ""), cell.getColumnIndex());
                    }
                    first = false;
                    continue;
                }
                
                boolean isEmptyRow = true;
                for (int i = 0; i < row.getLastCellNum(); i++) {
                    Cell cell = row.getCell(i);
                    if (cell != null && cell.getCellType() != CellType.BLANK) {
                        String value = formatter.formatCellValue(cell).trim();
                        if (!value.isEmpty()) {
                            isEmptyRow = false;
                            break;
                        }
                    }
                }
                
                if (isEmptyRow) continue;
                
                Map<String, String> rowData = new HashMap<>();
                for (Map.Entry<String, Integer> entry : headers.entrySet()) {
                    Cell cell = row.getCell(entry.getValue());
                    rowData.put(entry.getKey(), formatter.formatCellValue(cell));
                }
                
                String partIdStr = rowData.get("partid");
                String location = rowData.get("location");
                String quantityStr = rowData.get("quantity");
                
                if (partIdStr != null && !partIdStr.isEmpty() && 
                    location != null && !location.isEmpty() &&
                    quantityStr != null && !quantityStr.isEmpty()) {
                    InventoryItem item = buildInventoryItemFromMap(rowData);
                    items.add(item);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Falha ao ler planilha", e);
        }
        
        List<InventoryItem> saved = inventoryItemRepository.saveAll(items);
        saved.forEach(item -> {
            auditService.logEntityAction("CREATE", "INVENTORY_ITEM", item.getId(),
                    "Item cadastrado via planilha");
            inventoryAlertService.onStockLevelChanged(item);
        });
        return saved;
    }

    private InventoryItem buildInventoryItemFromMap(Map<String, String> data) {
        InventoryItem item = new InventoryItem();
        
        String partIdStr = data.get("partid");
        Long partId = Long.parseLong(partIdStr);
        Part part = partRepository.findById(partId)
                .orElseThrow(() -> new RuntimeException("Peça não encontrada: " + partId));
        item.setPart(part);
        
        item.setLocation(data.get("location"));
        
        String quantityStr = data.get("quantity");
        item.setQuantity(Integer.parseInt(quantityStr));
        
        String reservedStr = data.get("reservedquantity");
        if (reservedStr != null && !reservedStr.isEmpty()) {
            item.setReservedQuantity(Integer.parseInt(reservedStr));
        } else {
            item.setReservedQuantity(0);
        }
        
        String minStr = data.get("minimumquantity");
        if (minStr != null && !minStr.isEmpty()) {
            item.setMinimumQuantity(Integer.parseInt(minStr));
        } else {
            item.setMinimumQuantity(0);
        }
        
        String costStr = data.get("unitcost");
        if (costStr != null && !costStr.isEmpty()) {
            item.setUnitCost(new BigDecimal(costStr));
        }
        
        String priceStr = data.get("saleprice");
        if (priceStr != null && !priceStr.isEmpty()) {
            item.setSalePrice(new BigDecimal(priceStr));
        }
        
        return item;
    }

    public ByteArrayInputStream generateTemplate(String format) {
        if (format != null && (format.equalsIgnoreCase("xlsx") || format.equalsIgnoreCase("xls"))) {
            return generateTemplateExcel();
        }
        return generateTemplateCsv();
    }

    private ByteArrayInputStream generateTemplateCsv() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (CSVPrinter printer = new CSVPrinter(new PrintWriter(out),
                CSVFormat.DEFAULT.withHeader("partId", "location", "quantity", "reservedQuantity", "minimumQuantity", "unitCost", "salePrice"))) {
            printer.printRecord("1", "Prateleira A1", "50", "0", "10", "25.00", "45.00");
        } catch (IOException e) {
            throw new RuntimeException("Falha ao gerar template CSV", e);
        }
        return new ByteArrayInputStream(out.toByteArray());
    }

    private ByteArrayInputStream generateTemplateExcel() {
        try (Workbook workbook = WorkbookFactory.create(true); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("estoque");
            
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            
            CellStyle exampleStyle = workbook.createCellStyle();
            exampleStyle.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
            exampleStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            
            Row header = sheet.createRow(0);
            String[] headers = {"partId", "location", "quantity", "reservedQuantity", "minimumQuantity", "unitCost", "salePrice"};
            String[] headersDesc = {"ID da Peça*", "Localização*", "Quantidade*", "Qtd Reservada", "Qtd Mínima", "Custo Unit.", "Preço Venda"};
            
            for (int i = 0; i < headers.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(headersDesc[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 4000);
            }
            
            Row example = sheet.createRow(1);
            Object[] exampleData = {1, "Prateleira A1", 50, 0, 10, 25.00, 45.00};
            for (int i = 0; i < exampleData.length; i++) {
                Cell cell = example.createCell(i);
                if (exampleData[i] instanceof Number) {
                    cell.setCellValue(((Number) exampleData[i]).doubleValue());
                } else {
                    cell.setCellValue(String.valueOf(exampleData[i]));
                }
                cell.setCellStyle(exampleStyle);
            }
            
            Sheet instructionsSheet = workbook.createSheet("Instruções");
            int rowNum = 0;
            
            Row titleRow = instructionsSheet.createRow(rowNum++);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("📦 INSTRUÇÕES PARA IMPORTAÇÃO DE ITENS DE ESTOQUE");
            titleCell.setCellStyle(headerStyle);
            
            rowNum++;
            
            String[] instructions = {
                "1. CAMPOS OBRIGATÓRIOS:",
                "   • partId: ID da peça - OBRIGATÓRIO",
                "   • location: Localização física - OBRIGATÓRIO",
                "   • quantity: Quantidade em estoque - OBRIGATÓRIO",
                "",
                "2. FORMATO DOS CAMPOS:",
                "   • partId: Número inteiro (ID da peça cadastrada)",
                "   • location: Texto livre (ex: Prateleira A1)",
                "   • quantity: Número inteiro (quantidade atual)",
                "   • reservedQuantity: Número inteiro (qtd reservada)",
                "   • minimumQuantity: Número inteiro (estoque mínimo)",
                "   • unitCost: Valor decimal (ex: 25.00)",
                "   • salePrice: Valor decimal (ex: 45.00)",
                "",
                "3. COMO OBTER O ID DA PEÇA:",
                "   • Vá para a tela de Peças",
                "   • Exporte a lista de peças",
                "   • Use o ID da primeira coluna",
                "",
                "4. DICAS IMPORTANTES:",
                "   • Não altere os nomes das colunas",
                "   • A linha amarela é um exemplo",
                "   • Peça deve existir no sistema",
                "   • Use ponto para decimais",
                "   • Linhas vazias serão ignoradas",
                "",
                "5. APÓS PREENCHER:",
                "   • Salve o arquivo",
                "   • Vá para Estoque",
                "   • Clique em 'Importar'",
                "   • Selecione o arquivo"
            };
            
            for (String instruction : instructions) {
                Row row = instructionsSheet.createRow(rowNum++);
                row.createCell(0).setCellValue(instruction);
            }
            
            instructionsSheet.setColumnWidth(0, 20000);
            
            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException("Falha ao gerar template", e);
        }
    }
}
