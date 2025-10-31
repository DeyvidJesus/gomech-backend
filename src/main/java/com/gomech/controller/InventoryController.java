package com.gomech.controller;

import com.gomech.dto.Inventory.InventoryEntryRequestDTO;
import com.gomech.dto.Inventory.InventoryItemCreateDTO;
import com.gomech.dto.Inventory.InventoryItemResponseDTO;
import com.gomech.dto.Inventory.InventoryItemUpdateDTO;
import com.gomech.dto.Inventory.InventoryMovementResponseDTO;
import com.gomech.dto.Inventory.StockCancellationRequestDTO;
import com.gomech.dto.Inventory.StockConsumptionRequestDTO;
import com.gomech.dto.Inventory.StockReservationRequestDTO;
import com.gomech.dto.Inventory.StockReturnRequestDTO;
import com.gomech.service.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/inventory")
@Tag(name = "Estoque")
@SecurityRequirement(name = "bearer-key")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @Operation(summary = "Lista os itens de estoque cadastrados")
    @GetMapping("/items")
    public List<InventoryItemResponseDTO> listItems(@RequestParam(required = false) Long partId) {
        return inventoryService.listItems(partId);
    }

    @Operation(summary = "Busca detalhes de um item de estoque")
    @GetMapping("/items/{id}")
    public ResponseEntity<InventoryItemResponseDTO> getItem(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(inventoryService.getItem(id));
        } catch (RuntimeException ex) {
            throw translateException(ex);
        }
    }

    @Operation(summary = "Cria um novo item de estoque")
    @PostMapping("/items")
    public ResponseEntity<InventoryItemResponseDTO> createItem(@Valid @RequestBody InventoryItemCreateDTO dto) {
        try {
            InventoryItemResponseDTO response = inventoryService.createItem(dto);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException ex) {
            throw translateException(ex);
        }
    }

    @Operation(summary = "Atualiza um item de estoque existente")
    @PutMapping("/items/{id}")
    public ResponseEntity<InventoryItemResponseDTO> updateItem(@PathVariable Long id,
                                                               @Valid @RequestBody InventoryItemUpdateDTO dto) {
        try {
            return ResponseEntity.ok(inventoryService.updateItem(id, dto));
        } catch (RuntimeException ex) {
            throw translateException(ex);
        }
    }

    @Operation(summary = "Remove um item de estoque")
    @DeleteMapping("/items/{id}")
    public ResponseEntity<Void> deleteItem(@PathVariable Long id) {
        try {
            inventoryService.deleteItem(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException ex) {
            throw translateException(ex);
        }
    }

    @Operation(summary = "Registra uma entrada de estoque")
    @PostMapping("/movements/entry")
    public ResponseEntity<InventoryMovementResponseDTO> registerEntry(@Valid @RequestBody InventoryEntryRequestDTO dto) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(inventoryService.registerEntry(dto));
        } catch (RuntimeException ex) {
            throw translateException(ex);
        }
    }

    @Operation(summary = "Reserva itens para uma ordem de serviço")
    @PostMapping("/movements/reservations")
    public ResponseEntity<InventoryMovementResponseDTO> reserve(@Valid @RequestBody StockReservationRequestDTO dto) {
        try {
            return ResponseEntity.ok(inventoryService.reserveStock(dto));
        } catch (RuntimeException ex) {
            throw translateException(ex);
        }
    }

    @Operation(summary = "Efetua a baixa de itens reservados")
    @PostMapping("/movements/consumptions")
    public ResponseEntity<InventoryMovementResponseDTO> consume(@Valid @RequestBody StockConsumptionRequestDTO dto) {
        try {
            return ResponseEntity.ok(inventoryService.consumeStock(dto));
        } catch (RuntimeException ex) {
            throw translateException(ex);
        }
    }

    @Operation(summary = "Cancela uma reserva de estoque")
    @PostMapping("/movements/reservations/cancel")
    public ResponseEntity<InventoryMovementResponseDTO> cancelReservation(@Valid @RequestBody StockCancellationRequestDTO dto) {
        try {
            return ResponseEntity.ok(inventoryService.cancelReservation(dto));
        } catch (RuntimeException ex) {
            throw translateException(ex);
        }
    }

    @Operation(summary = "Devolve itens reservados para o estoque")
    @PostMapping("/movements/returns")
    public ResponseEntity<InventoryMovementResponseDTO> returnToStock(@Valid @RequestBody StockReturnRequestDTO dto) {
        try {
            return ResponseEntity.ok(inventoryService.returnToStock(dto));
        } catch (RuntimeException ex) {
            throw translateException(ex);
        }
    }

    @Operation(summary = "Lista movimentações de estoque")
    @GetMapping("/movements")
    public List<InventoryMovementResponseDTO> listMovements(@RequestParam(required = false) Long inventoryItemId,
                                                            @RequestParam(required = false) Long serviceOrderId,
                                                            @RequestParam(required = false) Long vehicleId) {
        return inventoryService.listMovements(inventoryItemId, serviceOrderId, vehicleId);
    }

    private ResponseStatusException translateException(RuntimeException ex) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        String message = ex.getMessage();
        if (message != null && message.toLowerCase().contains("não encontr")) {
            status = HttpStatus.NOT_FOUND;
        }
        return new ResponseStatusException(status, message, ex);
    }
}
