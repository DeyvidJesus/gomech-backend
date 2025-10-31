package com.gomech.service;

import com.gomech.integration.analytics.AnalyticsClient;
import com.gomech.integration.analytics.AnalyticsRequest;
import com.gomech.integration.analytics.AnalyticsResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AnalyticsService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AnalyticsService.class);

    private final AnalyticsClient analyticsClient;

    public AnalyticsService(AnalyticsClient analyticsClient) {
        this.analyticsClient = analyticsClient;
    }

    public AnalyticsResponse requestAnalytics(AnalyticsRequest request) {
        try {
            return analyticsClient.analyze(request);
        } catch (Exception e) {
            LOGGER.warn("Analytics service unavailable: {}", e.getMessage());
            return new AnalyticsResponse("ERROR", java.util.Map.of("message", "Analytics service unavailable"));
        }
    }
}
