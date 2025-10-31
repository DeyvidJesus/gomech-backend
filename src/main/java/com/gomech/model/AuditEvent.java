package com.gomech.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "audit_events")
@Getter
@Setter
@NoArgsConstructor
public class AuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "payload", columnDefinition = "TEXT")
    private String payload;

    @Column(name = "event_hash", nullable = false, length = 128)
    private String eventHash;

    @Column(name = "blockchain_reference", length = 128)
    private String blockchainReference;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public AuditEvent(String eventType, String payload, String eventHash, String blockchainReference) {
        this.eventType = eventType;
        this.payload = payload;
        this.eventHash = eventHash;
        this.blockchainReference = blockchainReference;
        this.createdAt = Instant.now();
    }
}
