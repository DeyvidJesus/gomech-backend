package com.gomech.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "crm_messages")
@NoArgsConstructor
@EntityListeners(com.gomech.listener.OrganizationEntityListener.class)
public class CrmMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @Column(name = "phone_number", nullable = false)
    private String phoneNumber;

    @Column(name = "message_text", columnDefinition = "TEXT")
    private String messageText;

    @Column(name = "message_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private MessageType messageType; // OUTBOUND, INBOUND

    @Column(name = "direction", nullable = false)
    @Enumerated(EnumType.STRING)
    private MessageDirection direction; // SENT, RECEIVED

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private MessageStatus status; // PENDING, SENT, DELIVERED, READ, FAILED

    @Column(name = "channel", nullable = false)
    private String channel; // WHATSAPP, SMS, EMAIL

    @Column(name = "whatsapp_message_id")
    private String whatsappMessageId;

    @Column(name = "sentiment")
    @Enumerated(EnumType.STRING)
    private Sentiment sentiment; // POSITIVE, NEUTRAL, NEGATIVE

    @Column(name = "sentiment_score")
    private Double sentimentScore;

    @Column(name = "is_automated")
    private Boolean isAutomated = false;

    @Column(name = "template_name")
    private String templateName;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    public enum MessageType {
        OUTBOUND, // Enviada pela oficina
        INBOUND   // Recebida do cliente
    }

    public enum MessageDirection {
        SENT,     // Enviada
        RECEIVED  // Recebida
    }

    public enum MessageStatus {
        PENDING,   // Aguardando envio
        SENT,      // Enviada
        DELIVERED, // Entregue
        READ,      // Lida
        FAILED     // Falha no envio
    }

    public enum Sentiment {
        POSITIVE,  // Cliente satisfeito
        NEUTRAL,   // Neutro
        NEGATIVE   // Cliente insatisfeito
    }

    public CrmMessage(Client client, String phoneNumber, String messageText, 
                     MessageType messageType, String channel) {
        this.client = client;
        this.phoneNumber = phoneNumber;
        this.messageText = messageText;
        this.messageType = messageType;
        this.channel = channel;
        this.direction = messageType == MessageType.OUTBOUND ? MessageDirection.SENT : MessageDirection.RECEIVED;
        this.status = messageType == MessageType.OUTBOUND ? MessageStatus.PENDING : MessageStatus.DELIVERED;
    }
}

