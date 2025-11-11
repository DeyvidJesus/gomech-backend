package com.gomech.controller;

import com.gomech.dto.Crm.*;
import com.gomech.service.CrmService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/crm")
public class CrmController {

    private final CrmService crmService;

    public CrmController(CrmService crmService) {
        this.crmService = crmService;
    }

    /**
     * Enviar mensagem para cliente
     */
    @PostMapping("/messages/send")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<CrmMessageDTO> sendMessage(@RequestBody SendMessageRequest request) {
        CrmMessageDTO message = crmService.sendMessage(request);
        return ResponseEntity.ok(message);
    }

    /**
     * Receber mensagem de cliente (webhook)
     */
    @PostMapping("/messages/receive")
    public ResponseEntity<CrmMessageDTO> receiveMessage(
            @RequestParam Long clientId,
            @RequestParam String phoneNumber,
            @RequestParam String messageText) {
        CrmMessageDTO message = crmService.receiveMessage(clientId, phoneNumber, messageText);
        return ResponseEntity.ok(message);
    }

    /**
     * Listar mensagens de um cliente
     */
    @GetMapping("/messages/client/{clientId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<List<CrmMessageDTO>> getClientMessages(@PathVariable Long clientId) {
        List<CrmMessageDTO> messages = crmService.getClientMessages(clientId);
        return ResponseEntity.ok(messages);
    }

    /**
     * Salvar feedback de cliente
     */
    @PostMapping("/feedbacks")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<ClientFeedbackDTO> saveFeedback(
            @RequestParam Long clientId,
            @RequestParam(required = false) Long serviceOrderId,
            @RequestParam(required = false) Integer rating,
            @RequestParam(required = false) Integer npsScore,
            @RequestParam(required = false) String feedbackText,
            @RequestParam String source) {
        ClientFeedbackDTO feedback = crmService.saveFeedback(
                clientId, serviceOrderId, rating, npsScore, feedbackText, source);
        return ResponseEntity.ok(feedback);
    }

    /**
     * Listar feedbacks de um cliente
     */
    @GetMapping("/feedbacks/client/{clientId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<List<ClientFeedbackDTO>> getClientFeedbacks(@PathVariable Long clientId) {
        List<ClientFeedbackDTO> feedbacks = crmService.getClientFeedbacks(clientId);
        return ResponseEntity.ok(feedbacks);
    }

    /**
     * Obter métricas de satisfação
     */
    @GetMapping("/metrics/satisfaction")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getSatisfactionMetrics(
            @RequestParam(defaultValue = "30") int days) {
        Double avgNPS = crmService.getAverageNPS(days);
        Double avgRating = crmService.getAverageRating(days);
        
        return ResponseEntity.ok(Map.of(
                "period_days", days,
                "average_nps", avgNPS != null ? avgNPS : 0,
                "average_rating", avgRating != null ? avgRating : 0
        ));
    }
}

