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

    @PostMapping
    public String chat(String prompt) {
        return chatClient.prompt()
                .user(prompt + "De acordo com os dados da loja, sejam eles .json ou .xls/.xlsx")
                .call()
                .content();
    }
}
