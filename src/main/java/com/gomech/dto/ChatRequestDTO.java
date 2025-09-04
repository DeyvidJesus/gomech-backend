package com.gomech.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ChatRequestDTO {

    @JsonProperty("prompt")
    private String prompt;
    
    @JsonProperty("includeChart")
    private Boolean includeChart = false;
    
    @JsonProperty("useEnhancedAi")
    private Boolean useEnhancedAi = false;

    public ChatRequestDTO() {}

    public ChatRequestDTO(String prompt) {
        this.prompt = prompt;
    }
    
    public ChatRequestDTO(String prompt, Boolean includeChart, Boolean useEnhancedAi) {
        this.prompt = prompt;
        this.includeChart = includeChart != null ? includeChart : false;
        this.useEnhancedAi = useEnhancedAi != null ? useEnhancedAi : false;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }
    
    public Boolean getIncludeChart() {
        return includeChart;
    }
    
    public void setIncludeChart(Boolean includeChart) {
        this.includeChart = includeChart != null ? includeChart : false;
    }
    
    public Boolean getUseEnhancedAi() {
        return useEnhancedAi;
    }
    
    public void setUseEnhancedAi(Boolean useEnhancedAi) {
        this.useEnhancedAi = useEnhancedAi != null ? useEnhancedAi : false;
    }
}
