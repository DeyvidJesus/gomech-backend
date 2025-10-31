package com.gomech.notification;

public interface NotificationGateway {

    void sendLowStockAlert(NotificationPayload payload);
}
