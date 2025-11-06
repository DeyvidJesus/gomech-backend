package com.gomech.repository;

import com.gomech.domain.InventoryItem;
import com.gomech.dto.Inventory.PartAvailabilityDTO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryItemRepository extends JpaRepository<InventoryItem, Long> {
    List<InventoryItem> findByPartId(Long partId);

    Optional<InventoryItem> findByPartIdAndLocation(Long partId, String location);

    @Query("""
            SELECT new com.gomech.dto.Inventory.PartAvailabilityDTO(
                p.id,
                p.name,
                p.sku,
                SUM(i.quantity),
                SUM(i.reservedQuantity),
                SUM(i.minimumQuantity),
                SUM(i.quantity) - SUM(i.reservedQuantity),
                (SELECT MAX(m.movementDate) FROM InventoryMovement m WHERE m.part = p)
            )
            FROM InventoryItem i
            JOIN i.part p
            GROUP BY p.id, p.name, p.sku
            """)
    List<PartAvailabilityDTO> findAggregatedAvailability();

    @Query("""
            SELECT new com.gomech.dto.Inventory.PartAvailabilityDTO(
                p.id,
                p.name,
                p.sku,
                SUM(i.quantity),
                SUM(i.reservedQuantity),
                SUM(i.minimumQuantity),
                SUM(i.quantity) - SUM(i.reservedQuantity),
                (SELECT MAX(m.movementDate) FROM InventoryMovement m WHERE m.part = p)
            )
            FROM InventoryItem i
            JOIN i.part p
            WHERE p.id = :partId
            GROUP BY p.id, p.name, p.sku
            """)
    Optional<PartAvailabilityDTO> findAggregatedAvailabilityByPart(@Param("partId") Long partId);

    @Query("""
            SELECT new com.gomech.dto.Inventory.PartAvailabilityDTO(
                p.id,
                p.name,
                p.sku,
                SUM(i.quantity),
                SUM(i.reservedQuantity),
                SUM(i.minimumQuantity),
                SUM(i.quantity) - SUM(i.reservedQuantity),
                (SELECT MAX(m.movementDate) FROM InventoryMovement m WHERE m.part = p AND m.vehicle.id = :vehicleId)
            )
            FROM InventoryItem i
            JOIN i.part p
            WHERE EXISTS (
                SELECT 1
                FROM InventoryMovement mv
                WHERE mv.part = p AND mv.vehicle.id = :vehicleId
            )
            GROUP BY p.id, p.name, p.sku
            """)
    List<PartAvailabilityDTO> findAggregatedAvailabilityByVehicle(@Param("vehicleId") Long vehicleId);

    @Query("""
            SELECT new com.gomech.dto.Inventory.PartAvailabilityDTO(
                p.id,
                p.name,
                p.sku,
                SUM(i.quantity),
                SUM(i.reservedQuantity),
                SUM(i.minimumQuantity),
                SUM(i.quantity) - SUM(i.reservedQuantity),
                (SELECT MAX(m.movementDate)
                 FROM InventoryMovement m
                 JOIN m.serviceOrder so
                 WHERE m.part = p AND so.client.id = :clientId)
            )
            FROM InventoryItem i
            JOIN i.part p
            WHERE EXISTS (
                SELECT 1
                FROM InventoryMovement mv
                JOIN mv.serviceOrder so
                WHERE mv.part = p AND so.client.id = :clientId
            )
            GROUP BY p.id, p.name, p.sku
            """)
    List<PartAvailabilityDTO> findAggregatedAvailabilityByClient(@Param("clientId") Long clientId);


    @Query("""
            SELECT new com.gomech.dto.Analytics.SupplierPriceStats(
                p.manufacturer,
                AVG(i.unitCost)
            )
            FROM InventoryItem i
            JOIN i.part p
            WHERE p.manufacturer IS NOT NULL
              AND i.unitCost IS NOT NULL
            GROUP BY p.manufacturer
            """)
    List<com.gomech.dto.Analytics.SupplierPriceStats> findAverageCostBySupplier();

}
