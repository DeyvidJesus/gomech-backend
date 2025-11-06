package com.gomech.service;

import com.gomech.dto.Ai.AiRequestDTO;
import com.gomech.dto.Ai.AiResponseDTO;
import com.gomech.dto.Inventory.InventoryConsumptionFeatureDTO;
import com.gomech.dto.Inventory.InventoryRecommendationDTO;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Service
public class PythonAiService {
    private final WebClient webClient;
    private final String pythonServiceUrl;

    public PythonAiService(WebClient.Builder webClientBuilder) {
        this.pythonServiceUrl = "http://localhost:5000";
        this.webClient = webClientBuilder
                .baseUrl(pythonServiceUrl)
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(20 * 1024 * 1024))
                .build();
    }

    /**
     * Faz uma pergunta ao serviço Python usando o endpoint /chat real
     * @param request Request com pergunta e configurações
     * @return Resposta da IA processada no Python
     */
    public AiResponseDTO askQuestion(AiRequestDTO request) {
        try {
            PythonChatRequest pythonRequest = new PythonChatRequest(
                request.getQuestion(),
                    request.getThreadId(),
                    request.getUserId()
            );
            
            PythonChatResponse pythonResponse = webClient.post()
                    .uri("/chat")
                    .bodyValue(pythonRequest)
                    .retrieve()
                    .bodyToMono(PythonChatResponse.class)
                    .timeout(Duration.ofMinutes(3))
                    .block();
            
            if (pythonResponse == null) {
                throw new NullPointerException("Python chat response is null");
            }

            return new AiResponseDTO(
                pythonResponse.reply,
                pythonResponse.image_base64,
                pythonResponse.thread_id,
                pythonResponse.videos
            );
                    
        } catch (Exception e) {
            throw new RuntimeException("Erro na comunicação com IA: " + e.getMessage(), e);
        }
    }

    public boolean isServiceAvailable() {
        try {
            String response = webClient.get()
                    .uri("/")
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(5))
                    .block();

            return response != null && response.contains("healthy");

        } catch (Exception e) {
            return false;
        }
    }

    public Object getDetailedStatus() {
        try {
            return webClient.get()
                    .uri("/status")
                    .retrieve()
                    .bodyToMono(Object.class)
                    .timeout(Duration.ofSeconds(10))
                    .block();

        } catch (Exception e) {
            return new Object() {
                public final String status = "error";
                public final String message = "Não foi possível conectar com o Python AI Service";
                public final String error = e.getMessage();
                public final String pythonServiceUrl = PythonAiService.this.pythonServiceUrl;
            };
        }
    }

    public List<InventoryRecommendationDTO> fetchInventoryRecommendations(Long vehicleId,
                                                                          Long serviceOrderId,
                                                                          Integer limit,
                                                                          List<String> pipelines,
                                                                          List<InventoryConsumptionFeatureDTO> features) {
        try {
            InventoryRecommendationRequest payload = new InventoryRecommendationRequest(
                    vehicleId,
                    serviceOrderId,
                    limit,
                    pipelines,
                    features
            );

            InventoryRecommendationResponse response = webClient.post()
                    .uri("/inventory/recommendations")
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(InventoryRecommendationResponse.class)
                    .timeout(Duration.ofSeconds(30))
                    .block();

            if (response == null || response.recommendations == null) {
                return List.of();
            }

            return response.recommendations.stream()
                    .map(item -> new InventoryRecommendationDTO(
                            item.partId,
                            item.partName,
                            item.partSku,
                            item.confidence != null ? item.confidence : 0.0,
                            item.reason,
                            false,
                            item.historicalQuantity != null ? item.historicalQuantity : 0L,
                            item.lastMovementDate
                    ))
                    .toList();

        } catch (Exception e) {
            throw new RuntimeException("Erro ao buscar recomendações da IA: " + e.getMessage(), e);
        }
    }

    public void publishInventoryFeatures(String scope,
                                         List<String> pipelines,
                                         List<InventoryConsumptionFeatureDTO> features) {
        if (features == null || features.isEmpty()) {
            return;
        }

        try {
            InventoryFeatureBatch payload = new InventoryFeatureBatch(scope, pipelines, features);
            webClient.post()
                    .uri("/inventory/features")
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(Void.class)
                    .timeout(Duration.ofSeconds(30))
                    .block();
        } catch (Exception e) {
            throw new RuntimeException("Erro ao publicar features de inventário para a IA: " + e.getMessage(), e);
        }
    }

    public List<String> listPipelines() {
        try {
            PipelineResponse response = webClient.get()
                    .uri("/pipelines")
                    .retrieve()
                    .bodyToMono(PipelineResponse.class)
                    .timeout(Duration.ofSeconds(10))
                    .block();

            if (response == null || response.pipelines == null) {
                return List.of();
            }

            return new ArrayList<>(response.pipelines);
        } catch (Exception e) {
            return List.of();
        }
    }

    private static class PythonChatRequest {
        public String message;
        public String thread_id;
        public Long user_id;

        public PythonChatRequest(String message, String threadId, Long userId) {
            this.message = message;
            this.thread_id = threadId;
            this.user_id = userId;
        }
    }

    private static class PythonChatResponse {
        public String reply;
        public String thread_id;
        public String image_base64;
        public String image_mime;
        public String chart;
        public List<Object> videos;
    }

    private static class InventoryRecommendationRequest {
        public Long vehicleId;
        public Long serviceOrderId;
        public Integer limit;
        public List<String> pipelines;
        public List<InventoryConsumptionFeatureDTO> features;

        public InventoryRecommendationRequest(Long vehicleId,
                                              Long serviceOrderId,
                                              Integer limit,
                                              List<String> pipelines,
                                              List<InventoryConsumptionFeatureDTO> features) {
            this.vehicleId = vehicleId;
            this.serviceOrderId = serviceOrderId;
            this.limit = limit;
            this.pipelines = pipelines;
            this.features = features;
        }
    }

    private static class InventoryRecommendationResponse {
        public List<InventoryRecommendationItem> recommendations;
    }

    private static class InventoryRecommendationItem {
        public Long partId;
        public String partName;
        public String partSku;
        public Double confidence;
        public String reason;
        public Long historicalQuantity;
        public java.time.LocalDateTime lastMovementDate;
    }

    private static class InventoryFeatureBatch {
        public String scope;
        public List<String> pipelines;
        public List<InventoryConsumptionFeatureDTO> features;

        public InventoryFeatureBatch(String scope,
                                     List<String> pipelines,
                                     List<InventoryConsumptionFeatureDTO> features) {
            this.scope = scope;
            this.pipelines = pipelines;
            this.features = features;
        }
    }

    private static class PipelineResponse {
        public List<String> pipelines;
    }
}