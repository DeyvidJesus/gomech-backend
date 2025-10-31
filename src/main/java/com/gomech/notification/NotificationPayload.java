package com.gomech.notification;

import java.time.LocalDateTime;
import java.util.List;

public record NotificationPayload(
        Long inventoryItemId,
        Long partId,
        String partName,
        String partSku,
        String location,
        int quantity,
        int reservedQuantity,
        int minimumQuantity,
        int availableQuantity,
        List<String> emailRecipients,
        List<String> pushTopics,
        LocalDateTime triggeredAt
) {
}
