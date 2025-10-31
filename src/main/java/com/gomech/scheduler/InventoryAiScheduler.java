package com.gomech.scheduler;

import com.gomech.service.InventoryRecommendationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class InventoryAiScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(InventoryAiScheduler.class);

    private final InventoryRecommendationService inventoryRecommendationService;

    public InventoryAiScheduler(InventoryRecommendationService inventoryRecommendationService) {
        this.inventoryRecommendationService = inventoryRecommendationService;
    }

    @Scheduled(cron = "0 15 2 * * *")
    public void syncVehicleConsumptionHistory() {
        LOGGER.info("Iniciando sincronização de histórico de consumo por veículo com o motor de IA");
        inventoryRecommendationService.publishVehicleHistory();
    }

    @Scheduled(cron = "0 45 2 * * *")
    public void syncServiceOrderConsumptionHistory() {
        LOGGER.info("Iniciando sincronização de histórico de consumo por ordem de serviço com o motor de IA");
        inventoryRecommendationService.publishServiceOrderHistory();
    }
}
