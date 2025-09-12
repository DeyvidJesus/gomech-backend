package com.gomech.dto.Ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class AiResponseDTO {
    
    @JsonProperty("answer")
    private String answer;
    
    @JsonProperty("chart")
    private String chart;

    @JsonProperty("threadId")
    private String threadId;
    
    public AiResponseDTO() {}
    
    public AiResponseDTO(String answer, String chart, String threadId) {
        this.answer = answer;
        this.chart = chart;
        this.threadId = threadId;
    }

}