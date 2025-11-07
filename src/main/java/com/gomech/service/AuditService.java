package com.gomech.service;

import com.gomech.dto.Audit.AuditEventRequest;
import com.gomech.model.AuditEvent;
import com.gomech.repository.AuditEventRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.Locale;

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
                request.entityId(),
                occurredAt,
                canonicalPayload,
                hash,
                blockchainReference
        );
        return auditEventRepository.save(event);
    }

    public Page<AuditEvent> listEvents(LocalDateTime startDate,
                                       LocalDateTime endDate,
                                       String actionType,
                                       String userEmail,
                                       Pageable pageable) {
        Specification<AuditEvent> specification = Specification.where(null);

        if (startDate != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.greaterThanOrEqualTo(root.get("occurredAt"), startDate));
        }

        if (endDate != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.lessThanOrEqualTo(root.get("occurredAt"), endDate));
        }

        if (actionType != null && !actionType.isBlank()) {
            String normalizedAction = actionType.toLowerCase(Locale.ROOT);
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(criteriaBuilder.lower(root.get("operation")), normalizedAction));
        }

        if (userEmail != null && !userEmail.isBlank()) {
            String normalizedUser = userEmail.toLowerCase(Locale.ROOT);
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(criteriaBuilder.lower(root.get("userEmail")), normalizedUser));
        }

        return auditEventRepository.findAll(specification, pageable);
    }

    public void logEntityAction(String actionType,
                                String entityType,
                                Long entityId,
                                String metadata) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        String userEmail = "system@gomech";
        String userRole = "SYSTEM";
        if (authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)) {
            userEmail = authentication.getName();
            userRole = authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .min(Comparator.naturalOrder())
                    .orElse("ROLE_USER");
        }

        AuditEventRequest request = new AuditEventRequest(
                entityType + "_" + actionType,
                actionType,
                userEmail,
                entityType,
                userRole,
                LocalDateTime.now(),
                metadata,
                entityId
        );

        registerEvent(request);
    }

    private String buildCanonicalPayload(AuditEventRequest request, LocalDateTime occurredAt, Instant registrationInstant) {
        String metadata = request.metadata() != null ? request.metadata() : "";
        return String.join("|",
                request.eventType(),
                request.operation(),
                request.userEmail(),
                request.moduleName(),
                request.userRole(),
                request.entityId() != null ? request.entityId().toString() : "",
                occurredAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                registrationInstant.toString(),
                metadata
        );
    }
}
