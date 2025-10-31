package com.gomech.service;

import com.gomech.dto.Inventory.InventoryConsumptionFeatureDTO;
import com.gomech.dto.Inventory.InventoryRecommendationDTO;
import com.gomech.dto.Inventory.InventoryRecommendationRequestDTO;
import com.gomech.dto.Inventory.PartConsumptionStats;
import com.gomech.repository.InventoryMovementRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class InventoryRecommendationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(InventoryRecommendationService.class);
    private static final String DEFAULT_PIPELINE = "inventory-recommendation";

    private final InventoryMovementRepository inventoryMovementRepository;
    private final PythonAiService pythonAiService;

    public InventoryRecommendationService(InventoryMovementRepository inventoryMovementRepository,
                                          PythonAiService pythonAiService) {
        this.inventoryMovementRepository = inventoryMovementRepository;
        this.pythonAiService = pythonAiService;
    }

    @Transactional(readOnly = true)
    public List<InventoryRecommendationDTO> getRecommendations(InventoryRecommendationRequestDTO request) {
        Objects.requireNonNull(request, "request must not be null");

        int limit = resolveLimit(request.limit());
        List<InventoryConsumptionFeatureDTO> features = loadFeatures(request);
        List<String> pipelines = listAvailablePipelines();

        try {
            List<InventoryRecommendationDTO> aiRecommendations = pythonAiService.fetchInventoryRecommendations(
                    request.vehicleId(),
                    request.serviceOrderId(),
                    limit,
                    pipelines,
                    features
            );

            if (aiRecommendations == null || aiRecommendations.isEmpty()) {
                LOGGER.info("AI retornou lista vazia, acionando fallback para recomendações de estoque");
                return buildFallbackRecommendations(request, limit);
            }

            return aiRecommendations;

        } catch (RuntimeException ex) {
            LOGGER.warn("Falha ao consultar serviço de IA, utilizando fallback. Causa: {}", ex.getMessage());
            return buildFallbackRecommendations(request, limit);
        }
    }

    @Transactional(readOnly = true)
    public void publishVehicleHistory() {
        List<InventoryConsumptionFeatureDTO> features = inventoryMovementRepository.findAllVehicleConsumptionFeatures();
        if (features.isEmpty()) {
            LOGGER.debug("Nenhum histórico de consumo por veículo encontrado para envio ao motor de IA");
            return;
        }

        publishFeatures("vehicle-history", features);
    }

    @Transactional(readOnly = true)
    public void publishServiceOrderHistory() {
        List<InventoryConsumptionFeatureDTO> features = inventoryMovementRepository.findAllServiceOrderConsumptionFeatures();
        if (features.isEmpty()) {
            LOGGER.debug("Nenhum histórico de consumo por ordem encontrado para envio ao motor de IA");
            return;
        }

        publishFeatures("service-order-history", features);
    }

    @Transactional(readOnly = true)
    public List<String> listAvailablePipelines() {
        List<String> pipelines = pythonAiService.listPipelines();
        if (pipelines == null || pipelines.isEmpty()) {
            return List.of(DEFAULT_PIPELINE);
        }
        return pipelines;
    }

    private void publishFeatures(String scope, List<InventoryConsumptionFeatureDTO> features) {
        try {
            pythonAiService.publishInventoryFeatures(scope, listAvailablePipelines(), features);
        } catch (RuntimeException ex) {
            LOGGER.warn("Não foi possível enviar features de {} para o motor de IA: {}", scope, ex.getMessage());
        }
    }

    private List<InventoryConsumptionFeatureDTO> loadFeatures(InventoryRecommendationRequestDTO request) {
        List<InventoryConsumptionFeatureDTO> features;
        if (request.serviceOrderId() != null) {
            features = inventoryMovementRepository.findServiceOrderConsumptionFeatures(request.serviceOrderId());
        } else if (request.vehicleId() != null) {
            features = inventoryMovementRepository.findVehicleConsumptionFeatures(request.vehicleId());
        } else {
            features = inventoryMovementRepository.findGlobalConsumptionFeatures();
        }

        if (features.isEmpty()) {
            List<PartConsumptionStats> stats = fetchStatsForContext(request);
            features = stats.stream()
                    .map(stat -> new InventoryConsumptionFeatureDTO(
                            stat.partId(),
                            stat.partName(),
                            stat.partSku(),
                            defaultValue(stat.totalQuantity()),
                            defaultValue(stat.distinctServiceOrders()),
                            defaultValue(stat.distinctVehicles()),
                            stat.lastMovementDate(),
                            request.vehicleId(),
                            request.serviceOrderId()
                    ))
                    .toList();
        }

        return features;
    }

    private List<InventoryRecommendationDTO> buildFallbackRecommendations(InventoryRecommendationRequestDTO request,
                                                                          int limit) {
        List<PartConsumptionStats> stats = fetchStatsForContext(request);

        if (stats.isEmpty()) {
            LOGGER.info("Nenhum dado de histórico disponível para o fallback de recomendações");
            return List.of();
        }

        return stats.stream()
                .sorted(Comparator.comparing(PartConsumptionStats::totalQuantity).reversed()
                        .thenComparing(PartConsumptionStats::lastMovementDate, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(limit)
                .map(stat -> new InventoryRecommendationDTO(
                        stat.partId(),
                        stat.partName(),
                        stat.partSku(),
                        0.0,
                        "Sugestão baseada em histórico recente de consumo",
                        true,
                        defaultValue(stat.totalQuantity()),
                        stat.lastMovementDate()
                ))
                .collect(Collectors.toList());
    }

    private List<PartConsumptionStats> fetchStatsForContext(InventoryRecommendationRequestDTO request) {
        if (request.serviceOrderId() != null) {
            return inventoryMovementRepository.findConsumptionStatsByServiceOrder(request.serviceOrderId());
        }
        if (request.vehicleId() != null) {
            return inventoryMovementRepository.findConsumptionStatsByVehicle(request.vehicleId());
        }
        return inventoryMovementRepository.findOverallConsumptionStats();
    }

    private int resolveLimit(Integer limit) {
        return limit != null && limit > 0 ? limit : 5;
    }

    private long defaultValue(Long value) {
        return value != null ? value : 0L;
    }
}
