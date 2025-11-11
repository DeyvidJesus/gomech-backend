package com.gomech.service;

import com.gomech.dto.Crm.*;
import com.gomech.model.*;
import com.gomech.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CrmService {

    private final CrmMessageRepository crmMessageRepository;
    private final ClientFeedbackRepository clientFeedbackRepository;
    private final ClientRepository clientRepository;
    private final WhatsAppService whatsAppService;
    private final PythonAiService pythonAiService;

    public CrmService(CrmMessageRepository crmMessageRepository,
                     ClientFeedbackRepository clientFeedbackRepository,
                     ClientRepository clientRepository,
                     WhatsAppService whatsAppService,
                     PythonAiService pythonAiService) {
        this.crmMessageRepository = crmMessageRepository;
        this.clientFeedbackRepository = clientFeedbackRepository;
        this.clientRepository = clientRepository;
        this.whatsAppService = whatsAppService;
        this.pythonAiService = pythonAiService;
    }

    @Transactional
    public CrmMessageDTO sendMessage(SendMessageRequest request) {
        Client client = clientRepository.findById(request.clientId())
                .orElseThrow(() -> new RuntimeException("Cliente não encontrado"));

        String phoneNumber = request.phoneNumber() != null ? request.phoneNumber() : client.getPhone();

        if (phoneNumber == null || phoneNumber.isBlank()) {
            throw new RuntimeException("Número de telefone não informado");
        }

        // Criar registro da mensagem
        CrmMessage message = new CrmMessage(
                client,
                phoneNumber,
                request.messageText(),
                CrmMessage.MessageType.OUTBOUND,
                request.channel() != null ? request.channel() : "WHATSAPP"
        );

        message.setIsAutomated(request.isAutomated() != null ? request.isAutomated() : false);
        message.setTemplateName(request.templateName());

        // Enviar via WhatsApp (ou outro canal)
        if ("WHATSAPP".equals(message.getChannel())) {
            try {
                String messageId = whatsAppService.sendMessage(phoneNumber, request.messageText());
                message.setWhatsappMessageId(messageId);
                message.setStatus(CrmMessage.MessageStatus.SENT);
                message.setSentAt(LocalDateTime.now());
            } catch (Exception e) {
                message.setStatus(CrmMessage.MessageStatus.FAILED);
                message.setErrorMessage(e.getMessage());
            }
        }

        CrmMessage savedMessage = crmMessageRepository.save(message);
        return CrmMessageDTO.fromEntity(savedMessage);
    }

    @Transactional
    public CrmMessageDTO receiveMessage(Long clientId, String phoneNumber, String messageText) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new RuntimeException("Cliente não encontrado"));

        CrmMessage message = new CrmMessage(
                client,
                phoneNumber,
                messageText,
                CrmMessage.MessageType.INBOUND,
                "WHATSAPP"
        );

        message.setStatus(CrmMessage.MessageStatus.DELIVERED);

        // Analisar sentimento via Python AI
        // try {
        //    var analysis = pythonAiService.analyzeCrmMessage(messageText, client.getName());
        //    message.setSentiment(CrmMessage.Sentiment.valueOf(analysis.get("sentiment")));
        //    message.setSentimentScore((Double) analysis.get("sentiment_score"));
        //} catch (Exception e) {
            // Fallback: neutro
        //    message.setSentiment(CrmMessage.Sentiment.NEUTRAL);
        //    message.setSentimentScore(0.5);
        //}

        CrmMessage savedMessage = crmMessageRepository.save(message);
        return CrmMessageDTO.fromEntity(savedMessage);
    }

    public List<CrmMessageDTO> getClientMessages(Long clientId) {
        return crmMessageRepository.findByClientIdOrderByCreatedAtDesc(clientId)
                .stream()
                .map(CrmMessageDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public ClientFeedbackDTO saveFeedback(Long clientId, Long serviceOrderId, 
                                         Integer rating, Integer npsScore, 
                                         String feedbackText, String source) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new RuntimeException("Cliente não encontrado"));

        ClientFeedback feedback = new ClientFeedback();
        feedback.setClient(client);
        feedback.setRating(rating);
        feedback.setNpsScore(npsScore);
        feedback.setFeedbackText(feedbackText);
        feedback.setSource(source);
        feedback.setFeedbackType(ClientFeedback.FeedbackType.SATISFACTION);

        // Analisar sentimento
        //if (feedbackText != null && !feedbackText.isBlank()) {
        //    try {
        //        var analysis = pythonAiService.analyzeCrmMessage(feedbackText, client.getName());
        //        feedback.setSentiment(CrmMessage.Sentiment.valueOf(analysis.get("sentiment")));
        //         feedback.setSentimentScore((Double) analysis.get("sentiment_score"));
        //    } catch (Exception e) {
        //         feedback.setSentiment(CrmMessage.Sentiment.NEUTRAL);
        //    }
        //}

        ClientFeedback savedFeedback = clientFeedbackRepository.save(feedback);
        return ClientFeedbackDTO.fromEntity(savedFeedback);
    }

    public List<ClientFeedbackDTO> getClientFeedbacks(Long clientId) {
        return clientFeedbackRepository.findByClientIdOrderByCreatedAtDesc(clientId)
                .stream()
                .map(ClientFeedbackDTO::fromEntity)
                .collect(Collectors.toList());
    }

    public Double getAverageNPS(int days) {
        LocalDateTime startDate = LocalDateTime.now().minusDays(days);
        return clientFeedbackRepository.calculateAverageNPS(startDate);
    }

    public Double getAverageRating(int days) {
        LocalDateTime startDate = LocalDateTime.now().minusDays(days);
        return clientFeedbackRepository.calculateAverageRating(startDate);
    }
}

