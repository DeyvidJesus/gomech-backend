package com.gomech.service;

import com.gomech.domain.InventoryItem;
import com.gomech.model.*;
import com.gomech.repository.*;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Funcionalidades:
 * - Cálculo de rentabilidade por serviço
 * - Identificação de gargalos operacionais
 * - Benchmark interno entre oficinas
 * - Geração de relatórios executivos
 * - Análise de tendências
 * - Score de saúde operacional
 */
@Service
public class ManagementReportService {

    private final ServiceOrderRepository serviceOrderRepository;
    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final ClientFeedbackRepository clientFeedbackRepository;

    public ManagementReportService(
            ServiceOrderRepository serviceOrderRepository,
            OrganizationRepository organizationRepository,
            UserRepository userRepository,
            InventoryItemRepository inventoryItemRepository,
            ClientFeedbackRepository clientFeedbackRepository) {
        this.serviceOrderRepository = serviceOrderRepository;
        this.organizationRepository = organizationRepository;
        this.userRepository = userRepository;
        this.inventoryItemRepository = inventoryItemRepository;
        this.clientFeedbackRepository = clientFeedbackRepository;
    }

    /**
     * Calcula rentabilidade por tipo de serviço.
     */
    public Map<String, Object> calculateProfitabilityByService(Integer days, String serviceTypeFilter) {
        LocalDateTime startDate = LocalDateTime.now().minusDays(days);
        
        List<ServiceOrder> orders = serviceOrderRepository.findAll().stream()
                .filter(o -> o.getCreatedAt().isAfter(startDate))
                .filter(o -> "COMPLETED".equals(o.getStatus()))
                .collect(Collectors.toList());

        Map<String, ServiceProfitability> profitabilityMap = new HashMap<>();

        for (ServiceOrder order : orders) {
            String serviceType = order.getServiceType() != null ? order.getServiceType() : "GENERAL";
            
            if (serviceTypeFilter != null && !serviceType.equals(serviceTypeFilter)) {
                continue;
            }

            profitabilityMap.putIfAbsent(serviceType, new ServiceProfitability(serviceType));
            ServiceProfitability prof = profitabilityMap.get(serviceType);

            double totalValue = order.getTotalCost() != null ? order.getTotalCost().doubleValue() : 0.0;
            double laborCost = order.getLaborCost() != null ? order.getLaborCost().doubleValue() : 0.0;
            double partsCost = order.getPartsCost() != null ? order.getPartsCost().doubleValue() : 0.0;

            prof.addOrder(totalValue, laborCost, partsCost);
        }

        List<Map<String, Object>> rankings = profitabilityMap.values().stream()
                .map(ServiceProfitability::toMap)
                .sorted((a, b) -> Double.compare(
                        (Double) b.get("margin_percent"),
                        (Double) a.get("margin_percent")))
                .collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("service_profitability", rankings);
        result.put("period_days", days);
        result.put("total_services_analyzed", rankings.stream().mapToInt(r -> (Integer) r.get("count")).sum());
        
        if (!rankings.isEmpty()) {
            result.put("top_profitable", rankings.get(0));
            result.put("least_profitable", rankings.get(rankings.size() - 1));
            
            double totalRevenue = rankings.stream().mapToDouble(r -> (Double) r.get("total_revenue")).sum();
            double totalProfit = rankings.stream().mapToDouble(r -> (Double) r.get("total_profit")).sum();
            result.put("overall_margin", totalRevenue > 0 ? (totalProfit / totalRevenue * 100) : 0);
        }

        return result;
    }

    /**
     * Identifica gargalos operacionais.
     */
    public Map<String, Object> identifyOperationalBottlenecks() {
        Map<String, Object> bottlenecks = new HashMap<>();
        List<Map<String, Object>> critical = new ArrayList<>();
        List<Map<String, Object>> warnings = new ArrayList<>();
        List<Map<String, Object>> opportunities = new ArrayList<>();
        int healthScore = 100;

        // Análise de OSs atrasadas
        List<ServiceOrder> allOrders = serviceOrderRepository.findAll();
        List<ServiceOrder> delayedOrders = allOrders.stream()
                .filter(o -> "PENDING".equals(o.getStatus()) || "IN_PROGRESS".equals(o.getStatus()))
                .filter(o -> {
                    long daysOpen = ChronoUnit.DAYS.between(o.getCreatedAt(), LocalDateTime.now());
                    return daysOpen > 7;
                })
                .collect(Collectors.toList());

        if (!delayedOrders.isEmpty()) {
            Map<String, Object> issue = new HashMap<>();
            issue.put("type", "delayed_orders");
            issue.put("severity", "HIGH");
            issue.put("count", delayedOrders.size());
            issue.put("description", delayedOrders.size() + " OSs pendentes há mais de 7 dias");
            issue.put("impact", "Insatisfação do cliente e perda de receita");
            issue.put("recommendation", "Priorizar conclusão de OSs antigas e revisar capacidade da equipe");
            critical.add(issue);
            healthScore -= 15;
        }

        // Análise de capacidade
        long inProgressCount = allOrders.stream()
                .filter(o -> "IN_PROGRESS".equals(o.getStatus()))
                .count();

        if (inProgressCount > 15) {
            Map<String, Object> warning = new HashMap<>();
            warning.put("type", "capacity_issue");
            warning.put("severity", "MEDIUM");
            warning.put("count", inProgressCount);
            warning.put("description", inProgressCount + " OSs em andamento simultaneamente");
            warning.put("impact", "Possível sobrecarga da equipe");
            warning.put("recommendation", "Considerar contratação temporária ou redistribuição de trabalho");
            warnings.add(warning);
            healthScore -= 10;
        }

        // Análise de técnicos sobrecarregados
        List<User> technicians = userRepository.findAll().stream()
                .filter(u -> u.getRole() != null && u.getRole().isTechnician())
                .toList();

        int overloadedTechs = 0;
        int idleTechs = 0;

        for (User tech : technicians) {
            long activeOrders = allOrders.stream()
                    .filter(o -> tech.equals(o.getTechnicianName()))
                    .filter(o -> "PENDING".equals(o.getStatus()) || "IN_PROGRESS".equals(o.getStatus()))
                    .count();

            if (activeOrders > 5) {
                overloadedTechs++;
            } else if (activeOrders == 0) {
                idleTechs++;
            }
        }

        if (overloadedTechs > 0) {
            Map<String, Object> issue = new HashMap<>();
            issue.put("type", "overloaded_technicians");
            issue.put("severity", "HIGH");
            issue.put("count", overloadedTechs);
            issue.put("description", overloadedTechs + " técnico(s) com mais de 5 OSs ativas");
            issue.put("impact", "Risco de erros e atrasos");
            issue.put("recommendation", "Redistribuir OSs e revisar balanceamento de carga");
            critical.add(issue);
            healthScore -= 15;
        }

        if (idleTechs > 0 && overloadedTechs == 0) {
            Map<String, Object> opp = new HashMap<>();
            opp.put("type", "idle_capacity");
            opp.put("severity", "LOW");
            opp.put("count", idleTechs);
            opp.put("description", idleTechs + " técnico(s) disponível(is)");
            opp.put("impact", "Capacidade ociosa");
            opp.put("recommendation", "Alocar novos serviços ou realizar manutenções preventivas");
            opportunities.add(opp);
        }

        // Análise de estoque
        List<InventoryItem> items = inventoryItemRepository.findAll();
        long outOfStock = items.stream()
                .filter(i -> i.getQuantity() <= i.getMinimumQuantity())
                .count();

        if (outOfStock > 0) {
            Map<String, Object> issue = new HashMap<>();
            issue.put("type", "stock_shortage");
            issue.put("severity", "HIGH");
            issue.put("count", outOfStock);
            issue.put("description", outOfStock + " peça(s) em falta ou abaixo do mínimo");
            issue.put("impact", "Atrasos em serviços por falta de peças");
            issue.put("recommendation", "Reposição urgente de estoque e revisão de pontos de reposição");
            critical.add(issue);
            healthScore -= 20;
        }

        // Determinar status
        String status;
        String statusMessage;
        if (healthScore >= 80) {
            status = "HEALTHY";
            statusMessage = "✅ Operação saudável";
        } else if (healthScore >= 60) {
            status = "WARNING";
            statusMessage = "⚠️ Alguns pontos de atenção";
        } else {
            status = "CRITICAL";
            statusMessage = "🚨 Gargalos críticos identificados";
        }

        bottlenecks.put("status", status);
        bottlenecks.put("status_message", statusMessage);
        bottlenecks.put("overall_health_score", healthScore);
        bottlenecks.put("critical", critical);
        bottlenecks.put("warnings", warnings);
        bottlenecks.put("opportunities", opportunities);

        return bottlenecks;
    }

    /**
     * Realiza benchmark interno entre organizações.
     */
    public Map<String, Object> performInternalBenchmark(String metric) {
        List<Organization> organizations = organizationRepository.findAll();

        if (organizations.size() < 2) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "É necessário pelo menos 2 organizações para benchmark");
            error.put("available_orgs", organizations.size());
            return error;
        }

        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        List<Map<String, Object>> orgMetrics = new ArrayList<>();

        for (Organization org : organizations) {
            List<ServiceOrder> orgOrders = serviceOrderRepository.findAll().stream()
                    .filter(o -> org.equals(o.getOrganization()))
                    .filter(o -> o.getCreatedAt().isAfter(thirtyDaysAgo))
                    .collect(Collectors.toList());

            long completedOrders = orgOrders.stream()
                    .filter(o -> "COMPLETED".equals(o.getStatus()))
                    .count();

            double monthlyRevenue = orgOrders.stream()
                    .filter(o -> "COMPLETED".equals(o.getStatus()))
                    .mapToDouble(o -> o.getTotalCost() != null ? o.getTotalCost().doubleValue() : 0)
                    .sum();

            double avgTicket = completedOrders > 0 ? monthlyRevenue / completedOrders : 0;

            List<ClientFeedback> feedbacks = clientFeedbackRepository.findAll().stream()
                    .filter(f -> org.equals(f.getOrganization()))
                    .filter(f -> f.getCreatedAt().isAfter(thirtyDaysAgo))
                    .collect(Collectors.toList());

            double avgSatisfaction = feedbacks.isEmpty() ? 0 :
                    feedbacks.stream()
                            .mapToDouble(ClientFeedback::getRating)
                            .average()
                            .orElse(0);

            long technicianCount = userRepository.findAll().stream()
                    .filter(u -> org.equals(u.getOrganization()))
                    .filter(u -> u.getRole() != null && u.getRole().isTechnician())
                    .count();

            double ordersPerTechnician = technicianCount > 0 ? (double) completedOrders / technicianCount : 0;

            Map<String, Object> metrics = new HashMap<>();
            metrics.put("organization_id", org.getId());
            metrics.put("organization_name", org.getName());
            metrics.put("monthly_revenue", monthlyRevenue);
            metrics.put("avg_ticket", avgTicket);
            metrics.put("completed_orders", completedOrders);
            metrics.put("client_satisfaction", avgSatisfaction);
            metrics.put("technician_count", technicianCount);
            metrics.put("orders_per_technician", ordersPerTechnician);

            orgMetrics.add(metrics);
        }

        // Calcular médias
        double avgRevenue = orgMetrics.stream().mapToDouble(m -> (Double) m.get("monthly_revenue")).average().orElse(0);
        double avgTicket = orgMetrics.stream().mapToDouble(m -> (Double) m.get("avg_ticket")).average().orElse(0);
        double avgSatisfaction = orgMetrics.stream().mapToDouble(m -> (Double) m.get("client_satisfaction")).average().orElse(0);

        // Ordenar por métrica escolhida
        Comparator<Map<String, Object>> comparator = (a, b) -> {
            switch (metric.toLowerCase()) {
                case "ticket":
                    return Double.compare((Double) b.get("avg_ticket"), (Double) a.get("avg_ticket"));
                case "satisfaction":
                    return Double.compare((Double) b.get("client_satisfaction"), (Double) a.get("client_satisfaction"));
                case "productivity":
                    return Double.compare((Double) b.get("orders_per_technician"), (Double) a.get("orders_per_technician"));
                default:
                    return Double.compare((Double) b.get("monthly_revenue"), (Double) a.get("monthly_revenue"));
            }
        };

        List<Map<String, Object>> ranking = orgMetrics.stream()
                .sorted(comparator)
                .collect(Collectors.toList());

        // Adicionar ranks
        for (int i = 0; i < ranking.size(); i++) {
            ranking.get(i).put("rank", i + 1);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("summary", Map.of(
                "total_organizations", organizations.size(),
                "avg_monthly_revenue", avgRevenue,
                "avg_ticket", avgTicket,
                "avg_satisfaction", avgSatisfaction
        ));
        result.put("rankings", Map.of("by_" + metric, ranking));
        result.put("leaders", Map.of(
                "by_" + metric, ranking.get(0)
        ));

        return result;
    }

    /**
     * Gera relatório executivo consolidado.
     */
    public Map<String, Object> generateReport(String type, String format, Integer days) {
        Map<String, Object> report = new HashMap<>();
        report.put("report_type", type);
        report.put("report_date", LocalDateTime.now().toString());
        report.put("period_days", days);

        switch (type.toLowerCase()) {
            case "profitability":
                report.put("data", calculateProfitabilityByService(days, null));
                break;
            case "operational":
            case "bottlenecks":
                report.put("data", identifyOperationalBottlenecks());
                break;
            case "benchmark":
                report.put("data", performInternalBenchmark("revenue"));
                break;
            case "executive":
                Map<String, Object> executive = new HashMap<>();
                executive.put("profitability", calculateProfitabilityByService(days, null));
                executive.put("operational_health", identifyOperationalBottlenecks());
                executive.put("dashboard", getExecutiveDashboard(days));
                report.put("data", executive);
                break;
            default:
                report.put("error", "Unknown report type: " + type);
        }

        return report;
    }

    /**
     * Gera relatório em formato CSV.
     */
    public String generateReportCsv(String type, Integer days) {
        StringBuilder csv = new StringBuilder();

        if ("profitability".equalsIgnoreCase(type)) {
            Map<String, Object> data = calculateProfitabilityByService(days, null);
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> profitability = (List<Map<String, Object>>) data.get("service_profitability");

            csv.append("Service Type,Count,Total Revenue,Total Costs,Total Profit,Margin %,Avg Revenue,Avg Profit\n");
            for (Map<String, Object> service : profitability) {
                csv.append(String.format("%s,%d,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f\n",
                        service.get("service_type"),
                        service.get("count"),
                        service.get("total_revenue"),
                        service.get("total_costs"),
                        service.get("total_profit"),
                        service.get("margin_percent"),
                        service.get("avg_revenue_per_service"),
                        service.get("avg_profit_per_service")));
            }
        }

        return csv.toString();
    }

    /**
     * Dashboard executivo com KPIs principais.
     */
    public Map<String, Object> getExecutiveDashboard(Integer days) {
        LocalDateTime startDate = LocalDateTime.now().minusDays(days);
        
        List<ServiceOrder> orders = serviceOrderRepository.findAll().stream()
                .filter(o -> o.getCreatedAt().isAfter(startDate))
                .collect(Collectors.toList());

        long totalOrders = orders.size();
        long completedOrders = orders.stream().filter(o -> "COMPLETED".equals(o.getStatus())).count();
        double totalRevenue = orders.stream()
                .filter(o -> "COMPLETED".equals(o.getStatus()))
                .mapToDouble(o -> o.getTotalCost() != null ? o.getTotalCost().doubleValue() : 0)
                .sum();
        double avgTicket = completedOrders > 0 ? totalRevenue / completedOrders : 0;

        Map<String, Object> dashboard = new HashMap<>();
        dashboard.put("period_days", days);
        dashboard.put("total_orders", totalOrders);
        dashboard.put("completed_orders", completedOrders);
        dashboard.put("completion_rate", totalOrders > 0 ? (completedOrders * 100.0 / totalOrders) : 0);
        dashboard.put("total_revenue", totalRevenue);
        dashboard.put("avg_ticket", avgTicket);
        dashboard.put("health_score", identifyOperationalBottlenecks().get("overall_health_score"));

        return dashboard;
    }

    /**
     * Análise de tendências.
     */
    public Map<String, Object> analyzeTrends(String metric, Integer forecastDays) {
        // Placeholder - implementação futura com regressão linear
        Map<String, Object> trends = new HashMap<>();
        trends.put("metric", metric);
        trends.put("forecast_days", forecastDays);
        trends.put("message", "Análise de tendências em desenvolvimento");
        return trends;
    }

    /**
     * Calcula score de saúde operacional.
     */
    public Map<String, Object> calculateHealthScore() {
        return identifyOperationalBottlenecks();
    }

    // Classe auxiliar para cálculo de rentabilidade
    private static class ServiceProfitability {
        private final String serviceType;
        private int count = 0;
        private double totalRevenue = 0;
        private double totalCosts = 0;
        private double totalLaborCost = 0;
        private double totalPartsCost = 0;

        public ServiceProfitability(String serviceType) {
            this.serviceType = serviceType;
        }

        public void addOrder(double revenue, double laborCost, double partsCost) {
            count++;
            totalRevenue += revenue;
            totalLaborCost += laborCost;
            totalPartsCost += partsCost;
            totalCosts += (laborCost + partsCost);
        }

        public Map<String, Object> toMap() {
            double profit = totalRevenue - totalCosts;
            double margin = totalRevenue > 0 ? (profit / totalRevenue * 100) : 0;
            double avgRevenue = count > 0 ? totalRevenue / count : 0;
            double avgProfit = count > 0 ? profit / count : 0;

            Map<String, Object> map = new HashMap<>();
            map.put("service_type", serviceType);
            map.put("count", count);
            map.put("total_revenue", totalRevenue);
            map.put("total_costs", totalCosts);
            map.put("total_profit", profit);
            map.put("margin_percent", margin);
            map.put("avg_revenue_per_service", avgRevenue);
            map.put("avg_profit_per_service", avgProfit);
            map.put("labor_cost", totalLaborCost);
            map.put("parts_cost", totalPartsCost);
            return map;
        }
    }
}

