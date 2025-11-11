package com.gomech.controller;

import com.gomech.service.ManagementReportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * FASE 9: Controller para análises gerenciais e relatórios estratégicos.
 * 
 * Endpoints:
 * - GET /management/profitability - Análise de rentabilidade por serviço
 * - GET /management/bottlenecks - Identificação de gargalos operacionais
 * - GET /management/benchmark - Benchmark interno entre oficinas
 * - GET /management/reports/{type} - Geração de relatórios (JSON/CSV/PDF)
 */
@RestController
@RequestMapping("/management")
public class ManagementController {

    private final ManagementReportService reportService;

    public ManagementController(ManagementReportService reportService) {
        this.reportService = reportService;
    }

    /**
     * Análise de rentabilidade por tipo de serviço.
     * 
     * Query params:
     * - days: Período de análise (padrão: 30 dias)
     * - serviceType: Filtrar por tipo específico (opcional)
     */
    @GetMapping("/profitability")
    public ResponseEntity<Map<String, Object>> getProfitabilityAnalysis(
            @RequestParam(defaultValue = "30") Integer days,
            @RequestParam(required = false) String serviceType) {
        
        Map<String, Object> analysis = reportService.calculateProfitabilityByService(days, serviceType);
        return ResponseEntity.ok(analysis);
    }

    /**
     * Identificação de gargalos operacionais.
     * 
     * Analisa:
     * - OSs atrasadas
     * - Técnicos sobrecarregados
     * - Peças em falta
     * - Capacidade ociosa
     */
    @GetMapping("/bottlenecks")
    public ResponseEntity<Map<String, Object>> getOperationalBottlenecks() {
        Map<String, Object> bottlenecks = reportService.identifyOperationalBottlenecks();
        return ResponseEntity.ok(bottlenecks);
    }

    /**
     * Benchmark interno entre oficinas.
     * Disponível apenas para ambientes multi-tenant com múltiplas organizações.
     * 
     * Query params:
     * - metric: Métrica para ordenação (revenue, ticket, satisfaction, productivity)
     */
    @GetMapping("/benchmark")
    public ResponseEntity<Map<String, Object>> getInternalBenchmark(
            @RequestParam(defaultValue = "revenue") String metric) {
        
        Map<String, Object> benchmark = reportService.performInternalBenchmark(metric);
        return ResponseEntity.ok(benchmark);
    }

    /**
     * Geração de relatórios gerenciais.
     * 
     * Tipos:
     * - executive: Resumo executivo completo
     * - profitability: Relatório de rentabilidade
     * - operational: Análise operacional
     * - benchmark: Comparativo entre oficinas
     * 
     * Formatos:
     * - json: Estrutura JSON (padrão)
     * - csv: Arquivo CSV para exportação
     * - pdf: Documento PDF (futuro)
     */
    @GetMapping("/reports/{type}")
    public ResponseEntity<?> generateReport(
            @PathVariable String type,
            @RequestParam(defaultValue = "json") String format,
            @RequestParam(defaultValue = "30") Integer days) {
        
        if ("csv".equalsIgnoreCase(format)) {
            String csvContent = reportService.generateReportCsv(type, days);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("text/csv"));
            headers.setContentDispositionFormData("attachment", type + "_report.csv");
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(csvContent);
        } else {
            Map<String, Object> report = reportService.generateReport(type, format, days);
            return ResponseEntity.ok(report);
        }
    }

    /**
     * Dashboard executivo com métricas consolidadas.
     * 
     * Retorna:
     * - KPIs principais
     * - Tendências
     * - Alertas críticos
     * - Recomendações prioritárias
     */
    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getExecutiveDashboard(
            @RequestParam(defaultValue = "30") Integer days) {
        
        Map<String, Object> dashboard = reportService.getExecutiveDashboard(days);
        return ResponseEntity.ok(dashboard);
    }

    /**
     * Análise de tendências e previsões.
     * 
     * Query params:
     * - metric: Métrica para análise (revenue, orders, clients)
     * - forecastDays: Dias para previsão (padrão: 30)
     */
    @GetMapping("/trends")
    public ResponseEntity<Map<String, Object>> getTrendsAnalysis(
            @RequestParam(defaultValue = "revenue") String metric,
            @RequestParam(defaultValue = "30") Integer forecastDays) {
        
        Map<String, Object> trends = reportService.analyzeTrends(metric, forecastDays);
        return ResponseEntity.ok(trends);
    }

    /**
     * Score de saúde operacional.
     * 
     * Retorna um score de 0-100 baseado em:
     * - Taxa de conclusão de OSs
     * - Satisfação do cliente
     * - Disponibilidade de estoque
     * - Produtividade da equipe
     */
    @GetMapping("/health-score")
    public ResponseEntity<Map<String, Object>> getOperationalHealthScore() {
        Map<String, Object> healthScore = reportService.calculateHealthScore();
        return ResponseEntity.ok(healthScore);
    }
}

