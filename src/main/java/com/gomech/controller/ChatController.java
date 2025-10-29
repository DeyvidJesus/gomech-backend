package com.gomech.controller;

import com.gomech.dto.Ai.ChatResponseDTO;
import com.gomech.model.Conversation;
import com.gomech.model.User;
import com.gomech.repository.ConversationRepository;
import com.gomech.repository.UserRepository;
import com.gomech.dto.Ai.ChatRequestDTO;
import com.gomech.dto.Ai.AiRequestDTO;
import com.gomech.dto.Ai.AiResponseDTO;
import com.gomech.service.PythonAiService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/ai/chat")
public class ChatController {

    private final PythonAiService pythonAiService;
    private final ConversationRepository conversationRepository;
    private final UserRepository userRepository;

    public ChatController(PythonAiService pythonAiService,
                          ConversationRepository conversationRepository,
                          UserRepository userRepository) {
        this.pythonAiService = pythonAiService;
        this.conversationRepository = conversationRepository;
        this.userRepository = userRepository;
    }

    @PostMapping
    public ResponseEntity<ChatResponseDTO> chat(@RequestBody ChatRequestDTO request) {
        try {
            if (request.getPrompt() == null || request.getPrompt().isBlank()) {
                return ResponseEntity.badRequest()
                        .body(new ChatResponseDTO(null, "Prompt não pode ser vazio", null, null, null));
            }

            User user = userRepository.findById(String.valueOf(request.getUserId()))
                    .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

            String existingThreadId = conversationRepository.findThreadIdByUserId(user.getId())
                    .orElse(null);

            AiRequestDTO aiRequest = new AiRequestDTO(
                    request.getPrompt(),
                    false,
                    existingThreadId,
                    request.getUserId()
            );

            AiResponseDTO aiResponse = pythonAiService.askQuestion(aiRequest);

            if (existingThreadId == null && aiResponse.getThreadId() != null) {
                conversationRepository.save(new Conversation(user, aiResponse.getThreadId()));
            }

            return ResponseEntity.ok(
                    new ChatResponseDTO(aiResponse.getAnswer(), "success", aiResponse.getThreadId(), aiResponse.getChart(), aiResponse.getVideos())
            );

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ChatResponseDTO(null, "Erro ao processar o prompt: " + e.getMessage(), null, null, null));
        }
    }

    @GetMapping("/status")
    public ResponseEntity<Object> getAiServiceStatus() {
        try {
            Object detailedStatus = pythonAiService.getDetailedStatus();

            return ResponseEntity.ok(new Object() {
                public final Object pythonServiceStatus = detailedStatus;
                public final boolean serviceAvailable = pythonAiService.isServiceAvailable();
                public final String backendStatus = "operational";
                public final String message = "Status obtido do Python AI Service";
            });

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(new Object() {
                        public final boolean serviceAvailable = false;
                        public final String backendStatus = "error";
                        public final String message = "Erro ao obter status: " + e.getMessage();
                    });
        }
    }
}
