package com.gomech.dto.Crm;

import com.gomech.model.CrmMessage;

import java.time.LocalDateTime;

public record CrmMessageDTO(
        Long id,
        Long clientId,
        String clientName,
        String phoneNumber,
        String messageText,
        String messageType,
        String direction,
        String status,
        String channel,
        String sentiment,
        Double sentimentScore,
        Boolean isAutomated,
        String templateName,
        LocalDateTime createdAt,
        LocalDateTime sentAt
) {
    public static CrmMessageDTO fromEntity(CrmMessage message) {
        return new CrmMessageDTO(
                message.getId(),
                message.getClient().getId(),
                message.getClient().getName(),
                message.getPhoneNumber(),
                message.getMessageText(),
                message.getMessageType().name(),
                message.getDirection().name(),
                message.getStatus().name(),
                message.getChannel(),
                message.getSentiment() != null ? message.getSentiment().name() : null,
                message.getSentimentScore(),
                message.getIsAutomated(),
                message.getTemplateName(),
                message.getCreatedAt(),
                message.getSentAt()
        );
    }
}

