package com.gomech.service;

import com.gomech.dto.Analytics.AnalyticsInsightDTO;
import com.gomech.dto.Analytics.ClientServiceGap;
import com.gomech.dto.Analytics.PartStockBalance;
import com.gomech.dto.Analytics.PartUsageRanking;
import com.gomech.dto.Analytics.SupplierPriceStats;
import com.gomech.repository.InventoryItemRepository;
import com.gomech.repository.InventoryMovementRepository;
import com.gomech.repository.ServiceOrderRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class AnalyticsInsightService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Locale LOCALE_PT_BR = new Locale("pt", "BR");

    private final InventoryMovementRepository inventoryMovementRepository;
    private final ServiceOrderRepository serviceOrderRepository;
    private final InventoryItemRepository inventoryItemRepository;

    public AnalyticsInsightService(InventoryMovementRepository inventoryMovementRepository,
                                   ServiceOrderRepository serviceOrderRepository,
                                   InventoryItemRepository inventoryItemRepository) {
        this.inventoryMovementRepository = inventoryMovementRepository;
        this.serviceOrderRepository = serviceOrderRepository;
        this.inventoryItemRepository = inventoryItemRepository;
    }

    public List<AnalyticsInsightDTO> generateInsights() {
        List<AnalyticsInsightDTO> insights = new ArrayList<>();
        topPartLastMonth().ifPresent(insights::add);
        highlightUnusedPurchases().ifPresent(insights::add);
        addClientsNeedingRevision(insights);
        servicePerformanceInsight().ifPresent(insights::add);
        supplierComparison().ifPresent(insights::add);
        return insights;
    }

    private Optional<AnalyticsInsightDTO> topPartLastMonth() {
        YearMonth currentMonth = YearMonth.from(LocalDate.now());
        YearMonth lastMonth = currentMonth.minusMonths(1);
        LocalDateTime start = lastMonth.atDay(1).atStartOfDay();
        LocalDateTime end = lastMonth.atEndOfMonth().atTime(LocalTime.MAX);
        List<PartUsageRanking> ranking = inventoryMovementRepository.findTopConsumedPartsBetween(start, end, PageRequest.of(0, 1));
        if (ranking.isEmpty()) {
            return Optional.empty();
        }
        PartUsageRanking top = ranking.get(0);
        String description = String.format("%s foi a peça que mais saiu no último mês, totalizando %d movimentações de saída.",
                top.partName(),
                top.totalQuantity());
        return Optional.of(new AnalyticsInsightDTO("Peça destaque do mês", description, "INVENTORY"));
    }

    private Optional<AnalyticsInsightDTO> highlightUnusedPurchases() {
        return inventoryMovementRepository.findPartsWithUnusedStock().stream()
                .sorted(Comparator.comparingLong(PartStockBalance::unusedQuantity).reversed())
                .findFirst()
                .filter(balance -> balance.unusedQuantity() > 0)
                .map(balance -> {
                    String description = String.format(
                            "A peça %s possui %d unidades compradas e apenas %d utilizadas. Atenção ao estoque parado.",
                            balance.partName(),
                            balance.quantityPurchased() != null ? balance.quantityPurchased() : 0,
                            balance.quantityUsed() != null ? balance.quantityUsed() : 0
                    );
                    return new AnalyticsInsightDTO("Peças compradas e não utilizadas", description, "INVENTORY");
                });
    }

    private void addClientsNeedingRevision(List<AnalyticsInsightDTO> insights) {
        LocalDateTime threshold = LocalDateTime.now().minusMonths(18);
        List<ClientServiceGap> clients = serviceOrderRepository.findClientsWithServiceGap(threshold);
        if (clients.isEmpty()) {
            return;
        }
        String description = clients.stream()
                .sorted(Comparator.comparing(ClientServiceGap::lastServiceDate))
                .limit(5)
                .map(client -> String.format("%s (última revisão em %s)",
                        client.clientName(),
                        client.lastServiceDate() != null ? client.lastServiceDate().format(DATE_FORMATTER) : "data indisponível"))
                .reduce((a, b) -> a + "; " + b)
                .map(list -> list + ".")
                .orElse("Clientes com revisões atrasadas identificados.");
        insights.add(new AnalyticsInsightDTO("Clientes para notificar",
                "Os seguintes clientes estão há mais de 18 meses sem revisão: " + description,
                "CUSTOMER"));
    }

    private Optional<AnalyticsInsightDTO> servicePerformanceInsight() {
        LocalDateTime now = LocalDateTime.now();
        YearMonth currentMonth = YearMonth.from(now);
        YearMonth previousMonth = currentMonth.minusMonths(1);
        LocalDateTime currentStart = currentMonth.atDay(1).atStartOfDay();
        LocalDateTime previousStart = previousMonth.atDay(1).atStartOfDay();
        LocalDateTime previousEnd = currentStart.minusNanos(1);

        long previousOrders = serviceOrderRepository.countCreatedBetween(previousStart, previousEnd);
        long currentOrders = serviceOrderRepository.countCreatedBetween(currentStart, now);

        BigDecimal previousRevenue = serviceOrderRepository.sumTotalCostBetween(previousStart, previousEnd);
        BigDecimal currentRevenue = serviceOrderRepository.sumTotalCostBetween(currentStart, now);

        if (previousOrders == 0 && previousRevenue.compareTo(BigDecimal.ZERO) == 0 && currentOrders == 0
                && currentRevenue.compareTo(BigDecimal.ZERO) == 0) {
            return Optional.empty();
        }

        double orderGrowth = calculateGrowth(previousOrders, currentOrders);
        double revenueGrowth = calculateGrowth(previousRevenue, currentRevenue);

        String description = String.format(
                "Neste mês você teve %d ordens de serviço (%s de variação) e a receita atingiu %s (%s de variação).",
                currentOrders,
                formatPercentage(orderGrowth),
                formatCurrency(currentRevenue),
                formatPercentage(revenueGrowth));

        return Optional.of(new AnalyticsInsightDTO("Performance do mês",
                description,
                "OPERATIONS"));
    }

    private Optional<AnalyticsInsightDTO> supplierComparison() {
        List<SupplierPriceStats> stats = inventoryItemRepository.findAverageCostBySupplier();
        List<SupplierPriceStats> filtered = stats.stream()
                .filter(s -> s.supplier() != null && s.averageUnitCost() != null)
                .sorted(Comparator.comparingDouble(SupplierPriceStats::averageUnitCost))
                .toList();
        if (filtered.size() < 2) {
            return Optional.empty();
        }
        SupplierPriceStats best = filtered.get(0);
        SupplierPriceStats worst = filtered.get(filtered.size() - 1);
        String description = String.format(
                "O fornecedor %s possui preços médios de %s, melhores do que %s, que pratica %s.",
                best.supplier(),
                formatCurrency(best.averageUnitCost()),
                worst.supplier(),
                formatCurrency(worst.averageUnitCost()));
        return Optional.of(new AnalyticsInsightDTO("Comparativo de fornecedores", description, "SUPPLIER"));
    }

    private double calculateGrowth(long previous, long current) {
        if (previous == 0) {
            return current > 0 ? 100.0 : 0.0;
        }
        return ((double) current - previous) / previous * 100.0;
    }

    private double calculateGrowth(BigDecimal previous, BigDecimal current) {
        if (previous == null || current == null) {
            return 0.0;
        }
        if (previous.compareTo(BigDecimal.ZERO) == 0) {
            return current.compareTo(BigDecimal.ZERO) > 0 ? 100.0 : 0.0;
        }
        BigDecimal diff = current.subtract(previous);
        return diff.divide(previous, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).doubleValue();
    }

    private String formatPercentage(double value) {
        return String.format(Locale.US, "%.2f%%", value);
    }

    private String formatCurrency(double value) {
        NumberFormat format = NumberFormat.getCurrencyInstance(LOCALE_PT_BR);
        return format.format(value);
    }

    private String formatCurrency(BigDecimal value) {
        NumberFormat format = NumberFormat.getCurrencyInstance(LOCALE_PT_BR);
        return format.format(value != null ? value : BigDecimal.ZERO);
    }
}
