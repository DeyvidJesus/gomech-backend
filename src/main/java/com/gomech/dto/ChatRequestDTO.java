package com.gomech.dto;

public class ChatRequestDTO {

    private String prompt;

    public ChatRequestDTO() {}

    public ChatRequestDTO(String prompt) {
        this.prompt = prompt;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }
}
