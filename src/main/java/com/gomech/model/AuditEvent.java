package com.gomech.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_events")
@Getter
@Setter
@NoArgsConstructor
public class AuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id")
    private Organization organization;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "payload", columnDefinition = "TEXT")
    private String payload;

    @Column(name = "operation", nullable = false)
    private String operation;

    @Column(name = "user_email", nullable = false)
    private String userEmail;

    @Column(name = "module_name", nullable = false)
    private String moduleName;

    @Column(name = "user_role", nullable = false)
    private String userRole;

    @Column(name = "entity_id")
    private Long entityId;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    @Column(name = "event_hash", nullable = false, length = 128)
    private String eventHash;

    @Column(name = "blockchain_reference", length = 128)
    private String blockchainReference;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public AuditEvent(String eventType,
                      String operation,
                      String userEmail,
                      String moduleName,
                      String userRole,
                      Long entityId,
                      LocalDateTime occurredAt,
                      String payload,
                      String eventHash,
                      String blockchainReference) {
        this.eventType = eventType;
        this.operation = operation;
        this.userEmail = userEmail;
        this.moduleName = moduleName;
        this.userRole = userRole;
        this.entityId = entityId;
        this.occurredAt = occurredAt;
        this.payload = payload;
        this.eventHash = eventHash;
        this.blockchainReference = blockchainReference;
        this.createdAt = Instant.now();
    }

    public AuditEvent(String eventType,
                      String operation,
                      String userEmail,
                      String moduleName,
                      String userRole,
                      Long entityId,
                      LocalDateTime occurredAt,
                      String payload,
                      String eventHash,
                      String blockchainReference,
                      Organization organization) {
        this.eventType = eventType;
        this.operation = operation;
        this.userEmail = userEmail;
        this.moduleName = moduleName;
        this.userRole = userRole;
        this.entityId = entityId;
        this.occurredAt = occurredAt;
        this.payload = payload;
        this.eventHash = eventHash;
        this.blockchainReference = blockchainReference;
        this.organization = organization;
        this.createdAt = Instant.now();
    }
}
