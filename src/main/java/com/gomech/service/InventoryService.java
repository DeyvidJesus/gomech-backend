package com.gomech.service;

import com.gomech.model.InventoryMovement;
import com.gomech.model.InventoryMovementType;
import com.gomech.model.Part;
import com.gomech.model.ServiceOrderItem;
import com.gomech.repository.InventoryMovementRepository;
import com.gomech.repository.PartRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class InventoryService {

    private final PartRepository partRepository;
    private final InventoryMovementRepository movementRepository;

    public InventoryService(PartRepository partRepository, InventoryMovementRepository movementRepository) {
        this.partRepository = partRepository;
        this.movementRepository = movementRepository;
    }

    public void reservePart(Long partId, int quantity, ServiceOrderItem item, String reason) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantidade para reserva deve ser positiva");
        }
        Part part = getPart(partId);
        part.reserve(quantity);
        registerMovement(part, InventoryMovementType.ADJUST, quantity, quantity,
                reason != null ? reason : "Reserva de estoque", item);
    }

    public void releaseReservation(Long partId, int quantity, ServiceOrderItem item, String reason) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantidade para liberação deve ser positiva");
        }
        Part part = getPart(partId);
        part.releaseReservation(quantity);
        registerMovement(part, InventoryMovementType.ADJUST, quantity, -quantity,
                reason != null ? reason : "Liberação de reserva", item);
    }

    public void consumeReservedPart(Long partId, int quantity, ServiceOrderItem item, String reason) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantidade para consumo deve ser positiva");
        }
        Part part = getPart(partId);
        part.releaseReservation(quantity);
        part.decreaseQuantity(quantity);
        registerMovement(part, InventoryMovementType.OUT, quantity, -quantity,
                reason != null ? reason : "Baixa de estoque", item);
    }

    public void returnPart(Long partId, int quantity, boolean reserveAfterReturn, ServiceOrderItem item, String reason) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantidade para devolução deve ser positiva");
        }
        Part part = getPart(partId);
        part.increaseQuantity(quantity);
        int reservedChange = 0;
        if (reserveAfterReturn) {
            part.reserve(quantity);
            reservedChange = quantity;
        }
        registerMovement(part, InventoryMovementType.IN, quantity, reservedChange,
                reason != null ? reason : "Devolução ao estoque", item);
    }

    private Part getPart(Long partId) {
        return partRepository.findById(partId)
                .orElseThrow(() -> new IllegalArgumentException("Peça não encontrada"));
    }

    private void registerMovement(Part part, InventoryMovementType type, int quantity, int reservedChange,
                                  String description, ServiceOrderItem item) {
        InventoryMovement movement = new InventoryMovement();
        movement.setPart(part);
        movement.setType(type);
        movement.setQuantity(quantity);
        movement.setReservedChange(reservedChange);
        movement.setDescription(description);
        if (item != null) {
            movement.setServiceOrderItemId(item.getId());
            if (item.getServiceOrder() != null) {
                movement.setServiceOrderId(item.getServiceOrder().getId());
                movement.setReferenceCode(item.getServiceOrder().getOrderNumber());
            }
        }
        movementRepository.save(movement);
    }
}
