package com.gomech.dto.Analytics;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record AnalyticsRequestDTO(@NotBlank String metric, @NotNull Map<String, Object> payload) {
}
