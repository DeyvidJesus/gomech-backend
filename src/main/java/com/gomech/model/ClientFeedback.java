package com.gomech.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "client_feedbacks")
@NoArgsConstructor
@EntityListeners(com.gomech.listener.OrganizationEntityListener.class)
public class ClientFeedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_order_id")
    private ServiceOrder serviceOrder;

    @Column(name = "rating")
    private Integer rating; // 1-5 estrelas

    @Column(name = "nps_score")
    private Integer npsScore; // 0-10 (Net Promoter Score)

    @Column(name = "feedback_text", columnDefinition = "TEXT")
    private String feedbackText;

    @Column(name = "sentiment")
    @Enumerated(EnumType.STRING)
    private CrmMessage.Sentiment sentiment;

    @Column(name = "sentiment_score")
    private Double sentimentScore;

    @Column(name = "feedback_type")
    @Enumerated(EnumType.STRING)
    private FeedbackType feedbackType;

    @Column(name = "source")
    private String source; // WHATSAPP, EMAIL, SYSTEM, PHONE

    @Column(name = "resolved")
    private Boolean resolved = false;

    @Column(name = "resolution_notes", columnDefinition = "TEXT")
    private String resolutionNotes;

    @Column(name = "resolved_by")
    private String resolvedBy;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public enum FeedbackType {
        SATISFACTION,  // Avaliação de satisfação
        COMPLAINT,     // Reclamação
        SUGGESTION,    // Sugestão
        COMPLIMENT,    // Elogio
        QUESTION       // Dúvida
    }

    public ClientFeedback(Client client, ServiceOrder serviceOrder, Integer rating, 
                         String feedbackText, String source) {
        this.client = client;
        this.serviceOrder = serviceOrder;
        this.rating = rating;
        this.feedbackText = feedbackText;
        this.source = source;
        this.feedbackType = FeedbackType.SATISFACTION;
    }
}

