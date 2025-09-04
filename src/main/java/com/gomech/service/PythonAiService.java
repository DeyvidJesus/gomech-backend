package com.gomech.service;

import com.gomech.dto.AiRequestDTO;
import com.gomech.dto.AiResponseDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;

@Service
public class PythonAiService {

    private final WebClient webClient;
    private final String pythonServiceUrl;

    public PythonAiService(WebClient.Builder webClientBuilder,
                          @Value("${gomech.ai.python-service.url:http://localhost:8000}") String pythonServiceUrl) {
        this.pythonServiceUrl = pythonServiceUrl;
        this.webClient = webClientBuilder
                .baseUrl(pythonServiceUrl)
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(20 * 1024 * 1024)) // 20MB para gráficos grandes
                .build();
    }

    /**
     * Faz uma pergunta ao serviço Python - TODA IA é processada no Python
     * @param request Request com pergunta e configurações
     * @param aiType Tipo de IA: "standard" ou "enhanced"
     * @return Resposta da IA processada no Python
     */
    public AiResponseDTO askQuestion(AiRequestDTO request, String aiType) {
        try {
            // Cria request expandido para o serviço Python
            PythonAiRequest pythonRequest = new PythonAiRequest(
                request.getQuestion(),
                request.getChart(),
                aiType
            );
            
            return webClient.post()
                    .uri("/ask")
                    .bodyValue(pythonRequest)
                    .retrieve()
                    .bodyToMono(AiResponseDTO.class)
                    .timeout(Duration.ofMinutes(3)) // Timeout de 3 minutos para processamento de IA
                    .block();
                    
        } catch (WebClientResponseException e) {
            throw new RuntimeException("Erro na comunicação com Python AI Service: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new RuntimeException("Erro inesperado no processamento de IA: " + e.getMessage(), e);
        }
    }

    /**
     * Faz pergunta usando IA padrão (endpoint específico)
     */
    public AiResponseDTO askStandardQuestion(AiRequestDTO request) {
        try {
            PythonAiRequest pythonRequest = new PythonAiRequest(
                request.getQuestion(),
                request.getChart(),
                "standard"
            );
            
            return webClient.post()
                    .uri("/ask/standard")
                    .bodyValue(pythonRequest)
                    .retrieve()
                    .bodyToMono(AiResponseDTO.class)
                    .timeout(Duration.ofMinutes(2))
                    .block();
                    
        } catch (Exception e) {
            throw new RuntimeException("Erro na IA padrão: " + e.getMessage(), e);
        }
    }

    /**
     * Faz pergunta usando IA aprimorada com RAG (endpoint específico)
     */
    public AiResponseDTO askEnhancedQuestion(AiRequestDTO request) {
        try {
            PythonAiRequest pythonRequest = new PythonAiRequest(
                request.getQuestion(),
                request.getChart(),
                "enhanced"
            );
            
            return webClient.post()
                    .uri("/ask/enhanced")
                    .bodyValue(pythonRequest)
                    .retrieve()
                    .bodyToMono(AiResponseDTO.class)
                    .timeout(Duration.ofMinutes(3))
                    .block();
                    
        } catch (Exception e) {
            throw new RuntimeException("Erro na IA aprimorada: " + e.getMessage(), e);
        }
    }

    /**
     * Verifica se o serviço Python está disponível
     */
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

    /**
     * Obtém informações detalhadas sobre o status do serviço Python
     */
    public Object getDetailedStatus() {
        try {
            return webClient.get()
                    .uri("/")
                    .retrieve()
                    .bodyToMono(Object.class)
                    .timeout(Duration.ofSeconds(10))
                    .block();
                    
        } catch (Exception e) {
            return new Object() {
                public final String status = "error";
                public final String message = "Não foi possível conectar com o Python AI Service";
                public final String error = e.getMessage();
            };
        }
    }

    /**
     * Classe interna para request ao serviço Python
     */
    private static class PythonAiRequest {
        public String question;
        public Boolean chart;
        public String ai_type;

        public PythonAiRequest(String question, Boolean chart, String aiType) {
            this.question = question;
            this.chart = chart != null ? chart : false;
            this.ai_type = aiType != null ? aiType : "standard";
        }
    }
} 