package com.gomech.repository;

import com.gomech.domain.InventoryMovement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InventoryMovementRepository extends JpaRepository<InventoryMovement, Long> {
    List<InventoryMovement> findByInventoryItemId(Long inventoryItemId);
    List<InventoryMovement> findByServiceOrderId(Long serviceOrderId);
    List<InventoryMovement> findByVehicleId(Long vehicleId);
}
