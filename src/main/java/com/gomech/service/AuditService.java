package com.gomech.service;

import com.gomech.model.AuditEvent;
import com.gomech.repository.AuditEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

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
    public AuditEvent registerEvent(String eventType, String payload) {
        Instant timestamp = Instant.now();
        String canonicalPayload = eventType + "|" + payload + "|" + timestamp.toString();
        String hash = encryptionService.sha256(canonicalPayload);
        String blockchainReference = blockchainService.publishAuditEvent(eventType, hash, payload, timestamp);
        AuditEvent event = new AuditEvent(eventType, payload, hash, blockchainReference);
        return auditEventRepository.save(event);
    }
}
