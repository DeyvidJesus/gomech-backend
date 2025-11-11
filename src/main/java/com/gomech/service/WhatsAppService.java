package com.gomech.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;
import java.util.UUID;

/**
 * Serviço de integração com WhatsApp Business API.
 * 
 * PLACEHOLDER: Implementação simulada para desenvolvimento.
 * Em produção, substituir por integração real com:
 * - WhatsApp Business Cloud API (Meta)
 * - Twilio WhatsApp API
 * - Ou outro provedor
 */
@Service
public class WhatsAppService {

    private final String whatsappApiUrl;
    private final String whatsappToken;
    private final WebClient webClient;
    private final boolean isProduction;

    public WhatsAppService(WebClient.Builder webClientBuilder,
                          @Value("${whatsapp.api.url:https://api.whatsapp.com/v1}") String whatsappApiUrl,
                          @Value("${whatsapp.api.token:dummy-token}") String whatsappToken,
                          @Value("${whatsapp.enabled:false}") boolean isProduction) {
        this.whatsappApiUrl = whatsappApiUrl;
        this.whatsappToken = whatsappToken;
        this.isProduction = isProduction;
        this.webClient = webClientBuilder.baseUrl(whatsappApiUrl).build();
    }

    /**
     * Envia mensagem via WhatsApp.
     * 
     * @param phoneNumber Número no formato internacional (ex: +5511999998888)
     * @param message Texto da mensagem
     * @return ID da mensagem enviada
     */
    public String sendMessage(String phoneNumber, String message) {
        if (!isProduction) {
            // Modo desenvolvimento: simular envio
            String messageId = "wamid." + UUID.randomUUID().toString();
            System.out.println("📱 [WhatsApp SIMULADO] Enviando para " + phoneNumber);
            System.out.println("💬 Mensagem: " + message);
            System.out.println("✅ ID: " + messageId);
            return messageId;
        }

        try {
            // Implementação real da API do WhatsApp
            var response = webClient.post()
                    .uri("/messages")
                    .header("Authorization", "Bearer " + whatsappToken)
                    .bodyValue(Map.of(
                            "messaging_product", "whatsapp",
                            "to", phoneNumber,
                            "type", "text",
                            "text", Map.of("body", message)
                    ))
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response != null && response.containsKey("messages")) {
                var messages = (java.util.List<?>) response.get("messages");
                if (!messages.isEmpty()) {
                    var firstMessage = (Map<?, ?>) messages.get(0);
                    return (String) firstMessage.get("id");
                }
            }

            throw new RuntimeException("Resposta inválida da API do WhatsApp");

        } catch (Exception e) {
            throw new RuntimeException("Erro ao enviar mensagem WhatsApp: " + e.getMessage(), e);
        }
    }

    /**
     * Envia template de mensagem pré-aprovado.
     * 
     * @param phoneNumber Número do destinatário
     * @param templateName Nome do template aprovado no WhatsApp Business
     * @param parameters Parâmetros do template
     * @return ID da mensagem enviada
     */
    public String sendTemplate(String phoneNumber, String templateName, String[] parameters) {
        if (!isProduction) {
            String messageId = "wamid." + UUID.randomUUID().toString();
            System.out.println("📱 [WhatsApp SIMULADO] Template: " + templateName);
            System.out.println("📞 Para: " + phoneNumber);
            return messageId;
        }

        // Implementação real para templates
        throw new UnsupportedOperationException("Templates não implementados ainda");
    }
}

