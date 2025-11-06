package com.gomech.controller;

import com.gomech.dto.ServiceOrder.ServiceOrderCreateDTO;
import com.gomech.dto.ServiceOrder.ServiceOrderResponseDTO;
import com.gomech.dto.ServiceOrder.ServiceOrderUpdateDTO;
import com.gomech.dto.ServiceOrder.ServiceOrderItemCreateDTO;
import com.gomech.dto.ServiceOrder.ServiceOrderItemResponseDTO;
import com.gomech.dto.ServiceOrder.UpdateStatusDTO;
import com.gomech.model.ServiceOrderStatus;
import com.gomech.service.ServiceOrderService;
import com.gomech.service.ServiceOrderItemService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/service-orders")
@CrossOrigin
public class ServiceOrderController {
    private static final Logger logger = LoggerFactory.getLogger(ServiceOrderController.class);
    @Autowired
    private ServiceOrderService serviceOrderService;

    @Autowired
    private ServiceOrderItemService itemService;

    @PostMapping
    public ResponseEntity<ServiceOrderResponseDTO> create(@RequestBody ServiceOrderCreateDTO dto) {
        logger.info(dto.toString());
        ServiceOrderResponseDTO created = serviceOrderService.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public ResponseEntity<List<ServiceOrderResponseDTO>> listAll() {
        return ResponseEntity.ok(serviceOrderService.listAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ServiceOrderResponseDTO> getById(@PathVariable Long id) {
        return serviceOrderService.getById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/order-number/{orderNumber}")
    public ResponseEntity<ServiceOrderResponseDTO> getByOrderNumber(@PathVariable String orderNumber) {
        return serviceOrderService.getByOrderNumber(orderNumber)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<ServiceOrderResponseDTO>> getByStatus(@PathVariable ServiceOrderStatus status) {
        return ResponseEntity.ok(serviceOrderService.getByStatus(status));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ServiceOrderResponseDTO> update(@PathVariable Long id, @RequestBody ServiceOrderUpdateDTO dto) {
        return ResponseEntity.ok(serviceOrderService.update(id, dto));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<ServiceOrderResponseDTO> updateStatus(@PathVariable Long id, @RequestBody UpdateStatusDTO dto) {
        return ResponseEntity.ok(serviceOrderService.updateStatus(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        serviceOrderService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/reports/overdue")
    public ResponseEntity<List<ServiceOrderResponseDTO>> getOverdueOrders() {
        return ResponseEntity.ok(serviceOrderService.getOverdueOrders());
    }

    @GetMapping("/reports/waiting-parts")
    public ResponseEntity<List<ServiceOrderResponseDTO>> getWaitingParts() {
        return ResponseEntity.ok(serviceOrderService.getWaitingParts());
    }

    @GetMapping("/reports/waiting-approval")
    public ResponseEntity<List<ServiceOrderResponseDTO>> getWaitingApproval() {
        return ResponseEntity.ok(serviceOrderService.getWaitingApproval());
    }

    @PostMapping("/{serviceOrderId}/items")
    public ResponseEntity<?> addItem(@PathVariable Long serviceOrderId, @RequestBody ServiceOrderItemCreateDTO dto) {
        try {
            logger.info("=== ENDPOINT CHAMADO === ServiceOrderId: {}, DTO: {}", serviceOrderId, dto);
            ServiceOrderItemResponseDTO item = itemService.addItem(serviceOrderId, dto);
            logger.info("=== ITEM CRIADO === {}", item);
            return ResponseEntity.status(HttpStatus.CREATED).body(item);
        } catch (Exception e) {
            logger.error("=== ERRO AO ADICIONAR ITEM ===", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(java.util.Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{serviceOrderId}/items")
    public ResponseEntity<List<ServiceOrderItemResponseDTO>> getItems(@PathVariable Long serviceOrderId) {
        List<ServiceOrderItemResponseDTO> items = itemService.getItemsByServiceOrder(serviceOrderId);
        return ResponseEntity.ok(items);
    }

    @PutMapping("/items/{itemId}")
    public ResponseEntity<ServiceOrderItemResponseDTO> updateItem(@PathVariable Long itemId, @RequestBody ServiceOrderItemCreateDTO dto) {
        try {
            ServiceOrderItemResponseDTO updated = itemService.updateItem(itemId, dto);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<Void> deleteItem(@PathVariable Long itemId) {
        try {
            itemService.deleteItem(itemId);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/items/{itemId}/apply")
    public ResponseEntity<ServiceOrderItemResponseDTO> applyItem(@PathVariable Long itemId) {
        try {
            ServiceOrderItemResponseDTO applied = itemService.applyItem(itemId);
            return ResponseEntity.ok(applied);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/items/{itemId}/unapply")
    public ResponseEntity<ServiceOrderItemResponseDTO> unapplyItem(@PathVariable Long itemId) {
        try {
            ServiceOrderItemResponseDTO unapplied = itemService.unapplyItem(itemId);
            return ResponseEntity.ok(unapplied);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // Endpoints para controle de estoque (preparação para módulo futuro)
    @PutMapping("/items/{itemId}/consume-stock")
    public ResponseEntity<ServiceOrderItemResponseDTO> consumeStock(@PathVariable Long itemId) {
        try {
            ServiceOrderItemResponseDTO consumed = itemService.consumeStock(itemId);
            return ResponseEntity.ok(consumed);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/items/{itemId}/return-stock")
    public ResponseEntity<ServiceOrderItemResponseDTO> returnStock(@PathVariable Long itemId) {
        try {
            ServiceOrderItemResponseDTO returned = itemService.returnStock(itemId);
            return ResponseEntity.ok(returned);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
