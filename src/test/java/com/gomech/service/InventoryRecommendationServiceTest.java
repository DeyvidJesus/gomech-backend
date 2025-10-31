package com.gomech.service;

import com.gomech.dto.Inventory.InventoryConsumptionFeatureDTO;
import com.gomech.dto.Inventory.InventoryRecommendationDTO;
import com.gomech.dto.Inventory.InventoryRecommendationRequestDTO;
import com.gomech.dto.Inventory.PartConsumptionStats;
import com.gomech.repository.InventoryMovementRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryRecommendationServiceTest {

    @Mock
    private InventoryMovementRepository inventoryMovementRepository;

    @Mock
    private PythonAiService pythonAiService;

    @InjectMocks
    private InventoryRecommendationService inventoryRecommendationService;

    private InventoryConsumptionFeatureDTO feature;
    private PartConsumptionStats stat1;
    private PartConsumptionStats stat2;

    @BeforeEach
    void setUp() {
        feature = new InventoryConsumptionFeatureDTO(1L, "Filtro", "FLT-001", 5L, 2L, 1L,
                LocalDateTime.now().minusDays(1), 200L, null);
        stat1 = new PartConsumptionStats(1L, "Filtro", "FLT-001", 5L, 2L, 1L,
                LocalDateTime.now().minusDays(1));
        stat2 = new PartConsumptionStats(2L, "Óleo", "OIL-001", 3L, 1L, 1L,
                LocalDateTime.now().minusDays(2));
    }

    @Test
    void shouldUseFallbackWhenAiUnavailable() {
        InventoryRecommendationRequestDTO request = new InventoryRecommendationRequestDTO(200L, null, 5);

        when(inventoryMovementRepository.findVehicleConsumptionFeatures(200L)).thenReturn(List.of(feature));
        when(inventoryMovementRepository.findConsumptionStatsByVehicle(200L)).thenReturn(List.of(stat1, stat2));
        when(pythonAiService.listPipelines()).thenReturn(List.of("inventory-recommendation"));
        when(pythonAiService.fetchInventoryRecommendations(eq(200L), isNull(), anyInt(), anyList(), anyList()))
                .thenThrow(new RuntimeException("AI indisponível"));

        List<InventoryRecommendationDTO> recommendations = inventoryRecommendationService.getRecommendations(request);

        assertThat(recommendations).hasSize(2);
        assertThat(recommendations.get(0).fromFallback()).isTrue();
        assertThat(recommendations.get(0).partId()).isEqualTo(1L);
    }

    @Test
    void shouldReturnAiRecommendationsWhenAvailable() {
        InventoryRecommendationRequestDTO request = new InventoryRecommendationRequestDTO(null, null, 3);
        InventoryRecommendationDTO aiRecommendation = new InventoryRecommendationDTO(3L, "Pastilha", "PST-001",
                0.92, "Alta probabilidade de troca", false, 9L, LocalDateTime.now());

        when(inventoryMovementRepository.findGlobalConsumptionFeatures()).thenReturn(List.of(feature));
        when(pythonAiService.listPipelines()).thenReturn(List.of("inventory-recommendation"));
        when(pythonAiService.fetchInventoryRecommendations(isNull(), isNull(), anyInt(), anyList(), anyList()))
                .thenReturn(List.of(aiRecommendation));

        List<InventoryRecommendationDTO> recommendations = inventoryRecommendationService.getRecommendations(request);

        assertThat(recommendations).containsExactly(aiRecommendation);
    }

    @Test
    void shouldPublishVehicleHistorySafely() {
        when(inventoryMovementRepository.findAllVehicleConsumptionFeatures()).thenReturn(List.of(feature));
        when(pythonAiService.listPipelines()).thenReturn(List.of("inventory-recommendation"));
        inventoryRecommendationService.publishVehicleHistory();
        // Success is absence of exception; verify interactions
        verify(pythonAiService)
                .publishInventoryFeatures(eq("vehicle-history"), anyList(), eq(List.of(feature)));
    }
}
