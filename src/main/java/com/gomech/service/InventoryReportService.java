package com.gomech.service;

import com.gomech.dto.Inventory.CriticalPartReportDTO;
import com.gomech.dto.Inventory.CriticalPartMovementProjection;
import com.gomech.dto.Inventory.PartAvailabilityDTO;
import com.gomech.dto.Inventory.PartConsumptionStats;
import com.gomech.repository.InventoryItemRepository;
import com.gomech.repository.InventoryMovementRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Transactional(readOnly = true)
public class InventoryReportService {

    private final InventoryItemRepository inventoryItemRepository;
    private final InventoryMovementRepository inventoryMovementRepository;

    public InventoryReportService(InventoryItemRepository inventoryItemRepository,
                                  InventoryMovementRepository inventoryMovementRepository) {
        this.inventoryItemRepository = inventoryItemRepository;
        this.inventoryMovementRepository = inventoryMovementRepository;
    }

    public List<CriticalPartReportDTO> listCriticalParts(String vehicleModel) {
        List<PartAvailabilityDTO> availability = inventoryItemRepository.findAggregatedAvailability();
        Map<Long, List<CriticalPartMovementProjection>> movementByPart = inventoryMovementRepository
                .findMovementAggregatesByVehicle(vehicleModel)
                .stream()
                .collect(Collectors.groupingBy(CriticalPartMovementProjection::partId));

        return availability.stream()
                .filter(dto -> dto.availableQuantity() <= dto.minimumQuantity())
                .flatMap(dto -> buildCriticalPartEntries(dto, movementByPart.get(dto.partId()), vehicleModel))
                .sorted(Comparator.comparing(CriticalPartReportDTO::availableQuantity)
                        .thenComparing(CriticalPartReportDTO::partName))
                .toList();
    }

    public PartAvailabilityDTO getAvailabilityForPart(Long partId) {
        return inventoryItemRepository.findAggregatedAvailabilityByPart(partId)
                .orElseThrow(() -> new IllegalArgumentException("Peça não encontrada no estoque"));
    }

    public List<PartAvailabilityDTO> listAvailabilityForVehicle(Long vehicleId) {
        return inventoryItemRepository.findAggregatedAvailabilityByVehicle(vehicleId);
    }

    public List<PartAvailabilityDTO> listAvailabilityForClient(Long clientId) {
        return inventoryItemRepository.findAggregatedAvailabilityByClient(clientId);
    }

    public List<PartConsumptionStats> getVehicleConsumptionHistory(Long vehicleId) {
        return inventoryMovementRepository.findConsumptionStatsByVehicle(vehicleId);
    }

    public List<PartConsumptionStats> getClientConsumptionHistory(Long clientId) {
        return inventoryMovementRepository.findConsumptionStatsByClient(clientId);
    }

    private Stream<CriticalPartReportDTO> buildCriticalPartEntries(PartAvailabilityDTO stock,
                                                                   List<CriticalPartMovementProjection> projections,
                                                                   String vehicleModel) {
        if (projections == null || projections.isEmpty()) {
            if (vehicleModel != null) {
                return Stream.empty();
            }
            return Stream.of(new CriticalPartReportDTO(
                    stock.partId(),
                    stock.partName(),
                    stock.partSku(),
                    "Sem Histórico",
                    stock.totalQuantity(),
                    stock.reservedQuantity(),
                    stock.minimumQuantity(),
                    stock.availableQuantity(),
                    0L,
                    stock.lastMovementDate()
            ));
        }

        return projections.stream()
                .map(projection -> new CriticalPartReportDTO(
                        stock.partId(),
                        stock.partName(),
                        stock.partSku(),
                        projection.vehicleModel(),
                        stock.totalQuantity(),
                        stock.reservedQuantity(),
                        stock.minimumQuantity(),
                        stock.availableQuantity(),
                        projection.totalConsumed(),
                        latestDate(stock.lastMovementDate(), projection.lastMovementDate())
                ));
    }

    private LocalDateTime latestDate(LocalDateTime stockDate, LocalDateTime movementDate) {
        if (movementDate == null) {
            return stockDate;
        }
        if (stockDate == null) {
            return movementDate;
        }
        return movementDate.isAfter(stockDate) ? movementDate : stockDate;
    }
}
