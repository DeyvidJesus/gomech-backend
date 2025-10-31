package com.gomech.repository;

import com.gomech.domain.InventoryMovement;
import com.gomech.dto.Inventory.InventoryConsumptionFeatureDTO;
import com.gomech.dto.Inventory.CriticalPartMovementProjection;
import com.gomech.dto.Inventory.PartConsumptionStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InventoryMovementRepository extends JpaRepository<InventoryMovement, Long> {
    List<InventoryMovement> findByInventoryItemId(Long inventoryItemId);
    List<InventoryMovement> findByServiceOrderId(Long serviceOrderId);
    List<InventoryMovement> findByVehicleId(Long vehicleId);

    @Query("""
            SELECT new com.gomech.dto.Inventory.InventoryConsumptionFeatureDTO(
                m.part.id,
                m.part.name,
                m.part.sku,
                SUM(m.quantity),
                COUNT(DISTINCT m.serviceOrder.id),
                COUNT(DISTINCT m.vehicle.id),
                MAX(m.movementDate),
                null,
                null
            )
            FROM InventoryMovement m
            WHERE m.movementType = com.gomech.domain.InventoryMovementType.OUT
            GROUP BY m.part.id, m.part.name, m.part.sku
            """)
    List<InventoryConsumptionFeatureDTO> findGlobalConsumptionFeatures();

    @Query("""
            SELECT new com.gomech.dto.Inventory.InventoryConsumptionFeatureDTO(
                m.part.id,
                m.part.name,
                m.part.sku,
                SUM(m.quantity),
                COUNT(DISTINCT m.serviceOrder.id),
                COUNT(DISTINCT m.vehicle.id),
                MAX(m.movementDate),
                m.vehicle.id,
                null
            )
            FROM InventoryMovement m
            WHERE m.movementType = com.gomech.domain.InventoryMovementType.OUT
              AND m.vehicle.id IS NOT NULL
            GROUP BY m.vehicle.id, m.part.id, m.part.name, m.part.sku
            """)
    List<InventoryConsumptionFeatureDTO> findAllVehicleConsumptionFeatures();

    @Query("""
            SELECT new com.gomech.dto.Inventory.InventoryConsumptionFeatureDTO(
                m.part.id,
                m.part.name,
                m.part.sku,
                SUM(m.quantity),
                COUNT(DISTINCT m.serviceOrder.id),
                COUNT(DISTINCT m.vehicle.id),
                MAX(m.movementDate),
                m.vehicle.id,
                null
            )
            FROM InventoryMovement m
            WHERE m.movementType = com.gomech.domain.InventoryMovementType.OUT
              AND m.vehicle.id = :vehicleId
            GROUP BY m.vehicle.id, m.part.id, m.part.name, m.part.sku
            """)
    List<InventoryConsumptionFeatureDTO> findVehicleConsumptionFeatures(Long vehicleId);

    @Query("""
            SELECT new com.gomech.dto.Inventory.InventoryConsumptionFeatureDTO(
                m.part.id,
                m.part.name,
                m.part.sku,
                SUM(m.quantity),
                COUNT(DISTINCT m.serviceOrder.id),
                COUNT(DISTINCT m.vehicle.id),
                MAX(m.movementDate),
                m.vehicle.id,
                m.serviceOrder.id
            )
            FROM InventoryMovement m
            WHERE m.movementType = com.gomech.domain.InventoryMovementType.OUT
              AND m.serviceOrder.id IS NOT NULL
            GROUP BY m.serviceOrder.id, m.vehicle.id, m.part.id, m.part.name, m.part.sku
            """)
    List<InventoryConsumptionFeatureDTO> findAllServiceOrderConsumptionFeatures();

    @Query("""
            SELECT new com.gomech.dto.Inventory.InventoryConsumptionFeatureDTO(
                m.part.id,
                m.part.name,
                m.part.sku,
                SUM(m.quantity),
                COUNT(DISTINCT m.serviceOrder.id),
                COUNT(DISTINCT m.vehicle.id),
                MAX(m.movementDate),
                m.vehicle.id,
                m.serviceOrder.id
            )
            FROM InventoryMovement m
            WHERE m.movementType = com.gomech.domain.InventoryMovementType.OUT
              AND m.serviceOrder.id = :serviceOrderId
            GROUP BY m.serviceOrder.id, m.vehicle.id, m.part.id, m.part.name, m.part.sku
            """)
    List<InventoryConsumptionFeatureDTO> findServiceOrderConsumptionFeatures(Long serviceOrderId);

    @Query("""
            SELECT new com.gomech.dto.Inventory.PartConsumptionStats(
                m.part.id,
                m.part.name,
                m.part.sku,
                SUM(m.quantity),
                COUNT(DISTINCT m.serviceOrder.id),
                COUNT(DISTINCT m.vehicle.id),
                MAX(m.movementDate)
            )
            FROM InventoryMovement m
            WHERE m.movementType = com.gomech.domain.InventoryMovementType.OUT
            GROUP BY m.part.id, m.part.name, m.part.sku
            """)
    List<PartConsumptionStats> findOverallConsumptionStats();

    @Query("""
            SELECT new com.gomech.dto.Inventory.PartConsumptionStats(
                m.part.id,
                m.part.name,
                m.part.sku,
                SUM(m.quantity),
                COUNT(DISTINCT m.serviceOrder.id),
                COUNT(DISTINCT m.vehicle.id),
                MAX(m.movementDate)
            )
            FROM InventoryMovement m
            WHERE m.movementType = com.gomech.domain.InventoryMovementType.OUT
              AND m.vehicle.id = :vehicleId
            GROUP BY m.part.id, m.part.name, m.part.sku
            """)
    List<PartConsumptionStats> findConsumptionStatsByVehicle(Long vehicleId);

    @Query("""
            SELECT new com.gomech.dto.Inventory.PartConsumptionStats(
                m.part.id,
                m.part.name,
                m.part.sku,
                SUM(m.quantity),
                COUNT(DISTINCT m.serviceOrder.id),
                COUNT(DISTINCT m.vehicle.id),
                MAX(m.movementDate)
            )
            FROM InventoryMovement m
            WHERE m.movementType = com.gomech.domain.InventoryMovementType.OUT
              AND m.serviceOrder.id = :serviceOrderId
            GROUP BY m.part.id, m.part.name, m.part.sku
            """)
    List<PartConsumptionStats> findConsumptionStatsByServiceOrder(Long serviceOrderId);

    @Query("""
            SELECT new com.gomech.dto.Inventory.PartConsumptionStats(
                m.part.id,
                m.part.name,
                m.part.sku,
                SUM(m.quantity),
                COUNT(DISTINCT m.serviceOrder.id),
                COUNT(DISTINCT m.vehicle.id),
                MAX(m.movementDate)
            )
            FROM InventoryMovement m
            JOIN m.serviceOrder so
            WHERE m.movementType = com.gomech.domain.InventoryMovementType.OUT
              AND so.client.id = :clientId
            GROUP BY m.part.id, m.part.name, m.part.sku
            """)
    List<PartConsumptionStats> findConsumptionStatsByClient(Long clientId);

    @Query("""
            SELECT new com.gomech.dto.Inventory.CriticalPartMovementProjection(
                m.part.id,
                COALESCE(v.model, 'Sem Histórico'),
                COALESCE(SUM(CASE WHEN m.movementType = com.gomech.domain.InventoryMovementType.OUT THEN m.quantity ELSE 0 END), 0),
                MAX(m.movementDate)
            )
            FROM InventoryMovement m
            LEFT JOIN m.vehicle v
            WHERE (:vehicleModel IS NULL OR (v.model IS NOT NULL AND UPPER(v.model) = UPPER(:vehicleModel)))
            GROUP BY m.part.id, COALESCE(v.model, 'Sem Histórico')
            """)
    List<CriticalPartMovementProjection> findMovementAggregatesByVehicle(@Param("vehicleModel") String vehicleModel);
}
