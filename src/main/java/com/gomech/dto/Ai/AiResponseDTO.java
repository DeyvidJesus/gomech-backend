package com.gomech.dto.Ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
public class AiResponseDTO {
    
    @JsonProperty("answer")
    private String answer;
    
    @JsonProperty("chart")
    private String chart;

    @JsonProperty("threadId")
    private String threadId;

    @JsonProperty("videos")
    private Object videos;
    
    public AiResponseDTO() {}
    
    public AiResponseDTO(String answer, String chart, String threadId, List<Object> videos) {
        this.answer = answer;
        this.chart = chart;
        this.threadId = threadId;
        this.videos = videos;
    }
}