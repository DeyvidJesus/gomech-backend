package com.gomech.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@ConditionalOnProperty(prefix = "notifications", name = "enabled", havingValue = "true")
public class NotificationGatewayRest implements NotificationGateway {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationGatewayRest.class);

    private final RestTemplate restTemplate;
    private final NotificationProperties properties;

    public NotificationGatewayRest(RestTemplateBuilder restTemplateBuilder, NotificationProperties properties) {
        this.restTemplate = restTemplateBuilder.build();
        this.properties = properties;
    }

    @Override
    public void sendLowStockAlert(NotificationPayload payload) {
        if (properties.getLowStockEndpoint() == null || properties.getLowStockEndpoint().isBlank()) {
            LOGGER.warn("Endpoint de notificações não configurado. Alerta será ignorado: {}", payload);
            return;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<NotificationPayload> request = new HttpEntity<>(payload, headers);
        try {
            restTemplate.postForEntity(properties.getLowStockEndpoint(), request, Void.class);
        } catch (Exception ex) {
            LOGGER.error("Falha ao enviar alerta de estoque crítico para o módulo de notificações", ex);
        }
    }
}
