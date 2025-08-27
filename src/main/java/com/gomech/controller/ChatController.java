package com.gomech.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.gomech.dto.ChatRequestDTO;

@RestController
@RequestMapping("/ai/chat")
public class ChatController {

    private final ChatClient chatClient;

    public ChatController(ChatClient.Builder builder, VectorStore vectorStore) {
        this.chatClient = builder
                .defaultAdvisors(new QuestionAnswerAdvisor(vectorStore))
                .build();
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

            String resposta = chatClient.prompt()
                    .user(request.getPrompt() + " De acordo com os dados da loja, sejam eles .json ou .xls/.xlsx")
                    .call()
                    .content();

            return ResponseEntity.ok(new ChatResponseDTO(resposta, "success"));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ChatResponseDTO(null, "Erro ao processar o prompt: " + e.getMessage()));
        }
    }
}
