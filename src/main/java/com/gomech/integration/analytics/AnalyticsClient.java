package com.gomech.integration.analytics;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "analyticsClient", url = "${analytics.service.url}", dismiss404 = true)
public interface AnalyticsClient {

    @PostMapping("/analyze")
    AnalyticsResponse analyze(@RequestBody AnalyticsRequest request);

    @GetMapping("/health")
    default String health() {
        return "UNKNOWN";
    }
}
