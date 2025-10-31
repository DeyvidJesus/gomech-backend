package com.gomech.controller;

import com.gomech.dto.Audit.AuditEventRequest;
import com.gomech.model.AuditEvent;
import com.gomech.service.AuditService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/audit")
public class AuditController {

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @PostMapping("/event")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AuditEvent> registerEvent(@RequestBody @Valid AuditEventRequest request) {
        AuditEvent event = auditService.registerEvent(request.eventType(), request.payload());
        return ResponseEntity.ok(event);
    }
}
