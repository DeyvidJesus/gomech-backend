package com.gomech.dto.Analytics;

public record PartUsageRanking(
        Long partId,
        String partName,
        Long totalQuantity
) {
}
