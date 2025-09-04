package com.gomech.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AiRequestDTO {
    
    @JsonProperty("question")
    private String question;
    
    @JsonProperty("chart")
    private Boolean chart = false;
    
    public AiRequestDTO() {}
    
    public AiRequestDTO(String question, Boolean chart) {
        this.question = question;
        this.chart = chart != null ? chart : false;
    }
    
    public String getQuestion() {
        return question;
    }
    
    public void setQuestion(String question) {
        this.question = question;
    }
    
    public Boolean getChart() {
        return chart;
    }
    
    public void setChart(Boolean chart) {
        this.chart = chart != null ? chart : false;
    }
} 