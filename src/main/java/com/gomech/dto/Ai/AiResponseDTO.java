package com.gomech.dto.Ai;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AiResponseDTO {
    
    @JsonProperty("answer")
    private String answer;
    
    @JsonProperty("chart")
    private String chart;
    
    public AiResponseDTO() {}
    
    public AiResponseDTO(String answer, String chart) {
        this.answer = answer;
        this.chart = chart;
    }
    
    public String getAnswer() {
        return answer;
    }
    
    public void setAnswer(String answer) {
        this.answer = answer;
    }
    
    public String getChart() {
        return chart;
    }
    
    public void setChart(String chart) {
        this.chart = chart;
    }
} 