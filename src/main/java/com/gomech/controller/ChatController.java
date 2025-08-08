package com.gomech.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
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

    @GetMapping
    public String chat() {
        return chatClient.prompt()
                .user("Gere um relatório com gráficos e descrições claras de como os clientes tem comprado, e a frequencia de suas compras e serviços. De acordo com os dados lidos no arquivo .json")
                .call()
                .content();
    }
}
