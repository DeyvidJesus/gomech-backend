package com.gomech.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.gomech.dto.ChatRequestDTO;
import com.gomech.dto.AiRequestDTO;
import com.gomech.dto.AiResponseDTO;
import com.gomech.service.PythonAiService;

@RestController
@RequestMapping("/ai/chat")
public class ChatController {

    private final PythonAiService pythonAiService;
    private final PythonAiService pythonAiService;

    public ChatController(PythonAiService pythonAiService) {
        this.pythonAiService = pythonAiService;
    }

    public static class ChatResponseDTO {
        private String content;
        private String status;

        public ChatResponseDTO(String content, String status) {
            this.content = content;
            this.status = status;
        }

        public String getContent() { return content; }
        public String getStatus() { return status; }
    }

        @PostMapping
    public ResponseEntity<EnhancedChatResponseDTO> chat(@RequestBody ChatRequestDTO request) {
        long startTime = System.currentTimeMillis();
        
        try {
            if (request.getPrompt() == null || request.getPrompt().isBlank()) {
                return ResponseEntity.badRequest()
                        .body(new EnhancedChatResponseDTO(null, "Prompt não pode ser vazio"));
            }

            // Cria request para o Python AI Service - sempre usando IA forte com RAG
            AiRequestDTO aiRequest = new AiRequestDTO(
                request.getPrompt() + " De acordo com os dados da loja, sejam eles .json ou .xls/.xlsx",
                false // chart = false por padrão no chat
            );

            // Usa sempre a IA aprimorada com RAG (enhanced)
            AiResponseDTO aiResponse = pythonAiService.askEnhancedQuestion(aiRequest);

            return ResponseEntity.ok(new ChatResponseDTO(aiResponse.getAnswer(), "success"));

        } catch (Exception e) {
            long processingTime = System.currentTimeMillis() - startTime;
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new EnhancedChatResponseDTO(null, "Erro ao processar o prompt: " + e.getMessage(), 
                          null, "error", processingTime));
        }
    }

    @GetMapping("/status")
    public ResponseEntity<Object> getAiServiceStatus() {
        boolean serviceAvailable = pythonAiService.isServiceAvailable();
        
        return ResponseEntity.ok(new Object() {
            public final boolean pythonAiServiceAvailable = serviceAvailable;
            public final boolean standardAiAvailable = serviceAvailable;
            public final boolean enhancedAiAvailable = serviceAvailable;
            public final String status = serviceAvailable ? "available" : "unavailable";
            public final String message = serviceAvailable ? 
                "Python AI Service está funcionando - Ambos os tipos de IA disponíveis" : 
                "Python AI Service não está disponível";
        });
    }
}
