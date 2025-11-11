package com.gomech.dto.Crm;

import com.gomech.model.ClientFeedback;

import java.time.LocalDateTime;

public record ClientFeedbackDTO(
        Long id,
        Long clientId,
        String clientName,
        Long serviceOrderId,
        Integer rating,
        Integer npsScore,
        String feedbackText,
        String sentiment,
        Double sentimentScore,
        String feedbackType,
        String source,
        Boolean resolved,
        LocalDateTime createdAt
) {
    public static ClientFeedbackDTO fromEntity(ClientFeedback feedback) {
        return new ClientFeedbackDTO(
                feedback.getId(),
                feedback.getClient().getId(),
                feedback.getClient().getName(),
                feedback.getServiceOrder() != null ? feedback.getServiceOrder().getId() : null,
                feedback.getRating(),
                feedback.getNpsScore(),
                feedback.getFeedbackText(),
                feedback.getSentiment() != null ? feedback.getSentiment().name() : null,
                feedback.getSentimentScore(),
                feedback.getFeedbackType().name(),
                feedback.getSource(),
                feedback.getResolved(),
                feedback.getCreatedAt()
        );
    }
}

