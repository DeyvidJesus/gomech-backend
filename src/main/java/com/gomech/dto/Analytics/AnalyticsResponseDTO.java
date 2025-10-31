package com.gomech.dto.Analytics;

import java.util.Map;

public record AnalyticsResponseDTO(String status, Map<String, Object> data) {
}
