package com.gomech.dto.Ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

<<<<<<< HEAD
import java.util.List;

=======
>>>>>>> 16690f57f3ffa06c233aefd0539beaa93b02f143
@Setter
@Getter
public class AiResponseDTO {
    
    @JsonProperty("answer")
    private String answer;
    
    @JsonProperty("chart")
    private String chart;

    @JsonProperty("threadId")
    private String threadId;
<<<<<<< HEAD

    @JsonProperty("videos")
    private Object videos;
    
    public AiResponseDTO() {}
    
    public AiResponseDTO(String answer, String chart, String threadId,  List<Object> videos) {
        this.answer = answer;
        this.chart = chart;
        this.threadId = threadId;
        this.videos = videos;
    }
=======
    
    public AiResponseDTO() {}
    
    public AiResponseDTO(String answer, String chart, String threadId) {
        this.answer = answer;
        this.chart = chart;
        this.threadId = threadId;
    }

>>>>>>> 16690f57f3ffa06c233aefd0539beaa93b02f143
}