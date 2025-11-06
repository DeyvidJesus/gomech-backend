package com.gomech.service;

import com.gomech.dto.Audit.AuditEventRequest;
import com.gomech.model.AuditEvent;
import com.gomech.repository.AuditEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class AuditService {

    private final AuditEventRepository auditEventRepository;
    private final EncryptionService encryptionService;
    private final BlockchainService blockchainService;

    public AuditService(AuditEventRepository auditEventRepository,
                        EncryptionService encryptionService,
                        BlockchainService blockchainService) {
        this.auditEventRepository = auditEventRepository;
        this.encryptionService = encryptionService;
        this.blockchainService = blockchainService;
    }

    @Transactional
    public AuditEvent registerEvent(AuditEventRequest request) {
        Instant registrationInstant = Instant.now();
        LocalDateTime occurredAt = request.occurredAt() != null ? request.occurredAt() : LocalDateTime.now();
        String canonicalPayload = buildCanonicalPayload(request, occurredAt, registrationInstant);
        String hash = encryptionService.sha256(canonicalPayload);
        String blockchainReference = blockchainService.publishAuditEvent(request.eventType(), hash, canonicalPayload, registrationInstant);
        AuditEvent event = new AuditEvent(
                request.eventType(),
                request.operation(),
                request.userEmail(),
                request.moduleName(),
                request.userRole(),
                occurredAt,
                canonicalPayload,
                hash,
                blockchainReference
        );
        return auditEventRepository.save(event);
    }

    public org.springframework.data.domain.Page<AuditEvent> listEvents(org.springframework.data.domain.Pageable pageable) {
        return auditEventRepository.findAll(pageable);
    }

    private String buildCanonicalPayload(AuditEventRequest request, LocalDateTime occurredAt, Instant registrationInstant) {
        String metadata = request.metadata() != null ? request.metadata() : "";
        return String.join("|",
                request.eventType(),
                request.operation(),
                request.userEmail(),
                request.moduleName(),
                request.userRole(),
                occurredAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                registrationInstant.toString(),
                metadata
        );
    }
}
