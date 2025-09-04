package com.gomech.service;

import com.gomech.dto.AiRequestDTO;
import com.gomech.dto.AiResponseDTO;
import com.gomech.controller.FileIngestionController;
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
                          @Value("${gomech.ai.python-service.url:http://localhost:5060}") String pythonServiceUrl) {
        this.pythonServiceUrl = pythonServiceUrl;
        this.webClient = webClientBuilder
                .baseUrl(pythonServiceUrl)
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(20 * 1024 * 1024)) // 20MB para gráficos grandes
                .build();
    }

    /**
     * Faz uma pergunta ao serviço Python usando IA forte com RAG
     * @param request Request com pergunta e configurações
     * @return Resposta da IA processada no Python com RAG
     */
    public AiResponseDTO askQuestion(AiRequestDTO request) {
        return askEnhancedQuestion(request);
    }


    /**
     * Faz pergunta usando IA aprimorada com RAG (sempre enhanced)
     */
    public AiResponseDTO askEnhancedQuestion(AiRequestDTO request) {
        try {
            // Usa construtor simplificado que sempre define enhanced
            PythonAiRequest pythonRequest = new PythonAiRequest(
                request.getQuestion(),
                request.getChart()
            );
            
            // Usa endpoint enhanced diretamente
            return webClient.post()
                    .uri("/ask/enhanced")
                    .bodyValue(pythonRequest)
                    .retrieve()
                    .bodyToMono(AiResponseDTO.class)
                    .timeout(Duration.ofMinutes(3))
                    .block();
                    
        } catch (Exception e) {
            throw new RuntimeException("Erro na IA com RAG: " + e.getMessage(), e);
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
     * Envia dados para ingestão no Python AI Service
     */
    public FileIngestionController.IngestionResponse ingestData(FileIngestionController.IngestionRequest request) {
        try {
            return webClient.post()
                    .uri("/ingest")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(FileIngestionController.IngestionResponse.class)
                    .timeout(Duration.ofMinutes(5))
                    .block();
                    
        } catch (Exception e) {
            throw new RuntimeException("Erro na ingestão de dados: " + e.getMessage(), e);
        }
    }

    /**
     * Limpa o vector store
     */
    public boolean clearVectorStore() {
        try {
            Map<String, String> response = webClient.post()
                    .uri("/ingest/clear")
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofMinutes(2))
                    .block();
                    
            return response != null && "success".equals(response.get("status"));
            
        } catch (Exception e) {
            throw new RuntimeException("Erro ao limpar vector store: " + e.getMessage(), e);
        }
    }

    /**
     * Obtém status da ingestão
     */
    public Map<String, Object> getIngestionStatus() {
        try {
            return webClient.get()
                    .uri("/ingest/status")
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(10))
                    .block();
                    
        } catch (Exception e) {
            throw new RuntimeException("Erro ao obter status de ingestão: " + e.getMessage(), e);
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
            this.ai_type = aiType != null ? aiType : "enhanced";
        }
        
        // Construtor simplificado que sempre usa enhanced
        public PythonAiRequest(String question, Boolean chart) {
            this.question = question;
            this.chart = chart != null ? chart : false;
            this.ai_type = "enhanced";
        }
    }
} 