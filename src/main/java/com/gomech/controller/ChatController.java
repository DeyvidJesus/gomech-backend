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
    public ResponseEntity<ChatResponseDTO> chat(@RequestBody ChatRequestDTO request) {
        try {
            if (request.getPrompt() == null || request.getPrompt().isBlank()) {
                return ResponseEntity.badRequest()
                        .body(new ChatResponseDTO(null, "Prompt não pode ser vazio"));
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
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ChatResponseDTO(null, "Erro ao processar o prompt: " + e.getMessage()));
        }
    }
}
