package com.gomech.dto.Ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.gomech.model.User;
import lombok.Getter;

@Getter
public class ChatRequestDTO {

    @JsonProperty("prompt")
    private String prompt;
    
    @JsonProperty("includeChart")
    private Boolean includeChart = false;

    @JsonProperty("threadId")
    private String threadId;

    @JsonProperty("userId")
    private Long userId;
}
