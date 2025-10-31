package com.gomech.service;

import com.gomech.model.InventoryMovement;
import com.gomech.model.InventoryMovementType;
import com.gomech.model.Part;
import com.gomech.repository.InventoryMovementRepository;
import com.gomech.repository.PartRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class PartService {

    private final PartRepository partRepository;
    private final InventoryMovementRepository movementRepository;

    public PartService(PartRepository partRepository, InventoryMovementRepository movementRepository) {
        this.partRepository = partRepository;
        this.movementRepository = movementRepository;
    }

    public Part registerPart(Part part) {
        partRepository.findByCode(part.getCode()).ifPresent(existing -> {
            throw new IllegalArgumentException("Já existe uma peça cadastrada com o código informado");
        });

        if (part.getQuantity() == null) {
            part.setQuantity(0);
        }
        if (part.getReservedQuantity() == null) {
            part.setReservedQuantity(0);
        }

        Part saved = partRepository.save(part);
        if (saved.getQuantity() != null && saved.getQuantity() > 0) {
            registerMovement(saved, InventoryMovementType.IN, saved.getQuantity(), 0, "Cadastro inicial de peça");
        }
        return saved;
    }

    public Part updatePart(Long id, Part updated) {
        Part existing = partRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Peça não encontrada"));

        if (updated.getName() != null) {
            existing.setName(updated.getName());
        }
        if (updated.getDescription() != null) {
            existing.setDescription(updated.getDescription());
        }
        if (updated.getUnitCost() != null) {
            existing.setUnitCost(updated.getUnitCost());
        }

        return partRepository.save(existing);
    }

    public Part adjustStock(Long partId, int quantityChange, String reason) {
        if (quantityChange == 0) {
            return partRepository.findById(partId)
                    .orElseThrow(() -> new IllegalArgumentException("Peça não encontrada"));
        }

        Part part = partRepository.findById(partId)
                .orElseThrow(() -> new IllegalArgumentException("Peça não encontrada"));

        int reserved = part.getReservedQuantity() != null ? part.getReservedQuantity() : 0;
        int newQuantity = (part.getQuantity() != null ? part.getQuantity() : 0) + quantityChange;
        if (newQuantity < reserved) {
            throw new IllegalStateException("Ajuste resultaria em estoque inferior à quantidade reservada");
        }
        if (newQuantity < 0) {
            throw new IllegalStateException("O ajuste resultaria em quantidade negativa em estoque");
        }

        part.setQuantity(newQuantity);
        Part saved = partRepository.save(part);

        InventoryMovementType type = quantityChange > 0 ? InventoryMovementType.IN : InventoryMovementType.OUT;
        registerMovement(saved, type, Math.abs(quantityChange), 0,
                reason != null ? reason : (quantityChange > 0 ? "Entrada de estoque" : "Saída de estoque"));
        return saved;
    }

    public Part reconcileStock(Long partId, int expectedQuantity, String reason) {
        Part part = partRepository.findById(partId)
                .orElseThrow(() -> new IllegalArgumentException("Peça não encontrada"));
        int reserved = part.getReservedQuantity() != null ? part.getReservedQuantity() : 0;
        if (expectedQuantity < reserved) {
            throw new IllegalStateException("Quantidade reconciliada não pode ser inferior às reservas atuais");
        }
        int currentQuantity = part.getQuantity() != null ? part.getQuantity() : 0;
        if (currentQuantity == expectedQuantity) {
            return part;
        }
        part.setQuantity(expectedQuantity);
        Part saved = partRepository.save(part);

        registerMovement(saved, InventoryMovementType.ADJUST, Math.abs(expectedQuantity - currentQuantity), 0,
                reason != null ? reason : "Conciliação de estoque");
        return saved;
    }

    public Part getPart(Long id) {
        return partRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Peça não encontrada"));
    }

    public List<Part> listAll() {
        return partRepository.findAll();
    }

    private void registerMovement(Part part, InventoryMovementType type, int quantity, int reservedChange, String description) {
        InventoryMovement movement = new InventoryMovement();
        movement.setPart(part);
        movement.setType(type);
        movement.setQuantity(quantity);
        movement.setReservedChange(reservedChange);
        movement.setDescription(description);
        movementRepository.save(movement);
    }
}
