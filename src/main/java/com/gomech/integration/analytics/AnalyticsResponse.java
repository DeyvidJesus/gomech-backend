package com.gomech.integration.analytics;

import java.util.Map;

public record AnalyticsResponse(String status, Map<String, Object> data) {
}
