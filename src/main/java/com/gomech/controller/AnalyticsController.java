package com.gomech.controller;

import com.gomech.dto.Analytics.AnalyticsRequestDTO;
import com.gomech.dto.Analytics.AnalyticsResponseDTO;
import com.gomech.integration.analytics.AnalyticsRequest;
import com.gomech.service.AnalyticsService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AnalyticsResponseDTO> analyze(@RequestBody @Valid AnalyticsRequestDTO requestDTO) {
        var response = analyticsService.requestAnalytics(new AnalyticsRequest(requestDTO.metric(), requestDTO.payload()));
        return ResponseEntity.ok(new AnalyticsResponseDTO(response.status(), response.data()));
    }
}
