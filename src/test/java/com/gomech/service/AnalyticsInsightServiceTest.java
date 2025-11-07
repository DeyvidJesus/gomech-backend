package com.gomech.service;

import com.gomech.dto.Analytics.AnalyticsInsightDTO;
import com.gomech.dto.Analytics.ClientServiceGap;
import com.gomech.dto.Analytics.PartStockBalance;
import com.gomech.dto.Analytics.PartUsageRanking;
import com.gomech.dto.Analytics.SupplierPriceStats;
import com.gomech.repository.InventoryItemRepository;
import com.gomech.repository.InventoryMovementRepository;
import com.gomech.repository.ServiceOrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsInsightServiceTest {

    @Mock
    private InventoryMovementRepository inventoryMovementRepository;

    @Mock
    private ServiceOrderRepository serviceOrderRepository;

    @Mock
    private InventoryItemRepository inventoryItemRepository;

    @InjectMocks
    private AnalyticsInsightService analyticsInsightService;

    @BeforeEach
    void setupMocks() {
        when(inventoryMovementRepository.findTopConsumedPartsBetween(any(), any(), any()))
                .thenReturn(List.of(new PartUsageRanking(1L, "Filtro de Ar", 42L)));

        when(inventoryMovementRepository.findPartsWithUnusedStock())
                .thenReturn(List.of(new PartStockBalance(2L, "Pastilha de Freio", 15L, 4L)));

        when(serviceOrderRepository.findClientsWithServiceGap(any()))
                .thenReturn(List.of(new ClientServiceGap(3L, "Cliente Z", "cliente@gomech.com",
                        LocalDateTime.now().minusMonths(19))));

        when(serviceOrderRepository.countCreatedBetween(any(), any()))
                .thenReturn(40L)
                .thenReturn(54L);

        when(serviceOrderRepository.sumTotalCostBetween(any(), any()))
                .thenReturn(BigDecimal.valueOf(10000))
                .thenReturn(BigDecimal.valueOf(14000));

        when(inventoryItemRepository.findAverageCostBySupplier())
                .thenReturn(List.of(
                        new SupplierPriceStats("Fornecedor Q", 85.0),
                        new SupplierPriceStats("Fornecedor K", 112.5)
                ));
    }

    @Test
    void generateInsightsProducesRequestedHighlights() {
        List<AnalyticsInsightDTO> insights = analyticsInsightService.generateInsights();

        assertThat(insights)
                .hasSizeGreaterThanOrEqualTo(4)
                .anyMatch(insight -> insight.description().contains("peça que mais saiu"))
                .anyMatch(insight -> insight.description().contains("estoque parado"))
                .anyMatch(insight -> insight.description().contains("18 meses"))
                .anyMatch(insight -> insight.description().contains("fornecedor"));
    }
}
