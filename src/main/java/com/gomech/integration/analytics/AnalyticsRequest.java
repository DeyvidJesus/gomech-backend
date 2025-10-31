package com.gomech.integration.analytics;

import java.util.Map;

public record AnalyticsRequest(String metric, Map<String, Object> payload) {
}
