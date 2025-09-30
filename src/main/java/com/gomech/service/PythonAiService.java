package com.gomech.service;

import com.gomech.dto.Ai.AiRequestDTO;
import com.gomech.dto.Ai.AiResponseDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
public class PythonAiService {
    private final WebClient webClient;
    private final String pythonServiceUrl;

    public PythonAiService(WebClient.Builder webClientBuilder) {
        // this.pythonServiceUrl = "https://gomech-ai-service.onrender.com";
        this.pythonServiceUrl = "http://localhost:8000";
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
}