package com.gomech.dto.Crm;

public record SendMessageRequest(
        Long clientId,
        String phoneNumber,
        String messageText,
        String channel, // WHATSAPP, SMS, EMAIL
        String templateName,
        Boolean isAutomated
) {}

