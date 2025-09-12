package com.gomech.dto.Ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.gomech.model.User;
import lombok.Getter;
import lombok.Setter;

@Getter
public class AiRequestDTO {
    
    @Setter
    @JsonProperty("question")
    private String question;
    
    @JsonProperty("chart")
    private Boolean chart = false;

    @JsonProperty("threadId")
    private String threadId;

    @JsonProperty("userId")
    private Long userId;

    public AiRequestDTO(String question, Boolean chart, String threadId, Long userId) {
        this.question = question;
        this.chart = chart != null ? chart : false;
        this.threadId = threadId;
        this.userId = userId;
    }
}