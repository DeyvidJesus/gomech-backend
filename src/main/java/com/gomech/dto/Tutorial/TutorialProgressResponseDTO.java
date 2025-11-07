package com.gomech.dto.Tutorial;

import com.gomech.model.TutorialProgress;

import java.time.LocalDateTime;
import java.util.List;

public record TutorialProgressResponseDTO(
    List<String> completedTutorials,
    String lastUpdatedAt
) {
    public static TutorialProgressResponseDTO fromEntity(TutorialProgress tutorialProgress) {
        String lastUpdated = null;
        
        if (tutorialProgress.getLastUpdatedAt() != null) {
            lastUpdated = tutorialProgress.getLastUpdatedAt().toString();
        } else if (tutorialProgress.getCreatedAt() != null) {
            lastUpdated = tutorialProgress.getCreatedAt().toString();
        } else {
            // Fallback se ambos forem null (não deveria acontecer após o fix do service)
            lastUpdated = LocalDateTime.now().toString();
        }
        
        return new TutorialProgressResponseDTO(
            tutorialProgress.getCompletedTutorials(),
            lastUpdated
        );
    }
}

