package com.gomech.notification;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "notifications")
public class NotificationProperties {

    /**
     * Flag que indica se a integração com o módulo de notificações está habilitada.
     */
    private boolean enabled = false;

    /**
     * Endpoint HTTP responsável por receber alertas de estoque crítico.
     */
    private String lowStockEndpoint = "";

    /**
     * Lista de destinatários padrão para alertas por e-mail.
     */
    private List<String> defaultEmailRecipients = new ArrayList<>();

    /**
     * Lista de tópicos/canais padrão para notificações push.
     */
    private List<String> defaultPushTopics = new ArrayList<>();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getLowStockEndpoint() {
        return lowStockEndpoint;
    }

    public void setLowStockEndpoint(String lowStockEndpoint) {
        this.lowStockEndpoint = lowStockEndpoint;
    }

    public List<String> getDefaultEmailRecipients() {
        return defaultEmailRecipients;
    }

    public void setDefaultEmailRecipients(List<String> defaultEmailRecipients) {
        this.defaultEmailRecipients = defaultEmailRecipients;
    }

    public List<String> getDefaultPushTopics() {
        return defaultPushTopics;
    }

    public void setDefaultPushTopics(List<String> defaultPushTopics) {
        this.defaultPushTopics = defaultPushTopics;
    }
}
