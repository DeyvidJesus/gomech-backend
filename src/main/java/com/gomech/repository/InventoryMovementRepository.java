package com.gomech.repository;

import com.gomech.model.InventoryMovement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InventoryMovementRepository extends JpaRepository<InventoryMovement, Long> {
    List<InventoryMovement> findByPartIdOrderByCreatedAtAsc(Long partId);
}
