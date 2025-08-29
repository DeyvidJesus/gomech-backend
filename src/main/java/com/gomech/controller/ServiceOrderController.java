package com.gomech.controller;

import com.gomech.dto.*;
import com.gomech.model.ServiceOrderStatus;
import com.gomech.service.ServiceOrderService;
import com.gomech.service.ServiceOrderItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/service-orders")
@CrossOrigin
public class ServiceOrderController {

    @Autowired
    private ServiceOrderService serviceOrderService;

    @Autowired
    private ServiceOrderItemService itemService;

    @PostMapping
    public ResponseEntity<ServiceOrderResponseDTO> create(@RequestBody ServiceOrderCreateDTO dto) {
        try {
            ServiceOrderResponseDTO created = serviceOrderService.create(dto);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping
    public ResponseEntity<List<ServiceOrderResponseDTO>> listAll() {
        List<ServiceOrderResponseDTO> serviceOrders = serviceOrderService.listAll();
        return ResponseEntity.ok(serviceOrders);
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
        List<ServiceOrderResponseDTO> serviceOrders = serviceOrderService.getByStatus(status);
        return ResponseEntity.ok(serviceOrders);
    }

    @GetMapping("/client/{clientId}")
    public ResponseEntity<List<ServiceOrderResponseDTO>> getByClientId(@PathVariable Long clientId) {
        List<ServiceOrderResponseDTO> serviceOrders = serviceOrderService.getByClientId(clientId);
        return ResponseEntity.ok(serviceOrders);
    }

    @GetMapping("/vehicle/{vehicleId}")
    public ResponseEntity<List<ServiceOrderResponseDTO>> getByVehicleId(@PathVariable Long vehicleId) {
        List<ServiceOrderResponseDTO> serviceOrders = serviceOrderService.getByVehicleId(vehicleId);
        return ResponseEntity.ok(serviceOrders);
    }

    @GetMapping("/vehicle/{vehicleId}/history")
    public ResponseEntity<List<ServiceOrderResponseDTO>> getVehicleHistory(@PathVariable Long vehicleId) {
        List<ServiceOrderResponseDTO> history = serviceOrderService.getVehicleHistory(vehicleId);
        return ResponseEntity.ok(history);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ServiceOrderResponseDTO> update(@PathVariable Long id, @RequestBody ServiceOrderUpdateDTO dto) {
        try {
            ServiceOrderResponseDTO updated = serviceOrderService.update(id, dto);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<ServiceOrderResponseDTO> updateStatus(@PathVariable Long id, @RequestBody ServiceOrderStatus status) {
        try {
            ServiceOrderResponseDTO updated = serviceOrderService.updateStatus(id, status);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        try {
            serviceOrderService.delete(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // Endpoints para relatórios
    @GetMapping("/reports/overdue")
    public ResponseEntity<List<ServiceOrderResponseDTO>> getOverdueOrders() {
        List<ServiceOrderResponseDTO> overdue = serviceOrderService.getOverdueOrders();
        return ResponseEntity.ok(overdue);
    }

    @GetMapping("/reports/waiting-parts")
    public ResponseEntity<List<ServiceOrderResponseDTO>> getWaitingParts() {
        List<ServiceOrderResponseDTO> waiting = serviceOrderService.getWaitingParts();
        return ResponseEntity.ok(waiting);
    }

    @GetMapping("/reports/waiting-approval")
    public ResponseEntity<List<ServiceOrderResponseDTO>> getWaitingApproval() {
        List<ServiceOrderResponseDTO> waiting = serviceOrderService.getWaitingApproval();
        return ResponseEntity.ok(waiting);
    }

    // Endpoints para gerenciar itens da OS
    @PostMapping("/{serviceOrderId}/items")
    public ResponseEntity<ServiceOrderItemResponseDTO> addItem(@PathVariable Long serviceOrderId, 
                                                              @RequestBody ServiceOrderItemCreateDTO dto) {
        try {
            ServiceOrderItemResponseDTO item = itemService.addItem(serviceOrderId, dto);
            return ResponseEntity.status(HttpStatus.CREATED).body(item);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{serviceOrderId}/items")
    public ResponseEntity<List<ServiceOrderItemResponseDTO>> getItems(@PathVariable Long serviceOrderId) {
        List<ServiceOrderItemResponseDTO> items = itemService.getItemsByServiceOrder(serviceOrderId);
        return ResponseEntity.ok(items);
    }

    @PutMapping("/items/{itemId}")
    public ResponseEntity<ServiceOrderItemResponseDTO> updateItem(@PathVariable Long itemId, 
                                                                 @RequestBody ServiceOrderItemCreateDTO dto) {
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
    @PutMapping("/items/{itemId}/reserve-stock")
    public ResponseEntity<ServiceOrderItemResponseDTO> reserveStock(@PathVariable Long itemId) {
        try {
            ServiceOrderItemResponseDTO reserved = itemService.reserveStock(itemId);
            return ResponseEntity.ok(reserved);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/items/{itemId}/release-stock")
    public ResponseEntity<ServiceOrderItemResponseDTO> releaseStock(@PathVariable Long itemId) {
        try {
            ServiceOrderItemResponseDTO released = itemService.releaseStock(itemId);
            return ResponseEntity.ok(released);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
