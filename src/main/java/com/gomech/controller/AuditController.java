package com.gomech.controller;

import com.gomech.dto.Audit.AuditEventRequest;
import com.gomech.dto.Audit.AuditEventResponse;
import com.gomech.model.AuditEvent;
import com.gomech.service.AuditService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

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
        AuditEvent event = auditService.registerEvent(request);
        return ResponseEntity.ok(event);
    }

    @GetMapping("/events")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<AuditEventResponse>> listEvents(
            @RequestParam(value = "startDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(value = "endDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(value = "actionType", required = false) String actionType,
            @RequestParam(value = "userEmail", required = false) String userEmail,
            Pageable pageable) {
        Page<AuditEventResponse> response = auditService.listEvents(startDate, endDate, actionType, userEmail, pageable)
                .map(AuditEventResponse::fromEntity);
        return ResponseEntity.ok(response);
    }
}
