package com.gomech.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class EnhancedChatResponseDTO {
    
    @JsonProperty("content")
    private String content;
    
    @JsonProperty("status")
    private String status;
    
    @JsonProperty("chart")
    private String chart; // Base64 encoded chart
    
    @JsonProperty("aiType")
    private String aiType; // "standard" ou "enhanced"
    
    @JsonProperty("processingTime")
    private Long processingTime; // tempo em ms
    
    public EnhancedChatResponseDTO() {}
    
    public EnhancedChatResponseDTO(String content, String status) {
        this.content = content;
        this.status = status;
        this.aiType = "standard";
    }
    
    public EnhancedChatResponseDTO(String content, String status, String chart, String aiType, Long processingTime) {
        this.content = content;
        this.status = status;
        this.chart = chart;
        this.aiType = aiType;
        this.processingTime = processingTime;
    }
    
    // Getters and Setters
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getChart() {
        return chart;
    }
    
    public void setChart(String chart) {
        this.chart = chart;
    }
    
    public String getAiType() {
        return aiType;
    }
    
    public void setAiType(String aiType) {
        this.aiType = aiType;
    }
    
    public Long getProcessingTime() {
        return processingTime;
    }
    
    public void setProcessingTime(Long processingTime) {
        this.processingTime = processingTime;
    }
} 