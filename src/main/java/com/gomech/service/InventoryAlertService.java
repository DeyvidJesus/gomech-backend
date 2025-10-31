package com.gomech.service;

import com.gomech.domain.InventoryItem;
import com.gomech.notification.NotificationGateway;
import com.gomech.notification.NotificationPayload;
import com.gomech.notification.NotificationProperties;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class InventoryAlertService {

    private final NotificationGateway notificationGateway;
    private final NotificationProperties notificationProperties;

    public InventoryAlertService(NotificationGateway notificationGateway,
                                 NotificationProperties notificationProperties) {
        this.notificationGateway = notificationGateway;
        this.notificationProperties = notificationProperties;
    }

    public void onStockLevelChanged(InventoryItem item) {
        if (item == null || item.getPart() == null) {
            return;
        }

        int available = item.getQuantity() - item.getReservedQuantity();
        if (available > item.getMinimumQuantity()) {
            return;
        }

        List<String> emailRecipients = notificationProperties.getDefaultEmailRecipients() == null
                ? List.of()
                : List.copyOf(notificationProperties.getDefaultEmailRecipients());
        List<String> pushTopics = notificationProperties.getDefaultPushTopics() == null
                ? List.of()
                : List.copyOf(notificationProperties.getDefaultPushTopics());

        NotificationPayload payload = new NotificationPayload(
                item.getId(),
                item.getPart().getId(),
                item.getPart().getName(),
                item.getPart().getSku(),
                item.getLocation(),
                item.getQuantity(),
                item.getReservedQuantity(),
                item.getMinimumQuantity(),
                available,
                emailRecipients,
                pushTopics,
                LocalDateTime.now()
        );
        notificationGateway.sendLowStockAlert(payload);
    }
}
