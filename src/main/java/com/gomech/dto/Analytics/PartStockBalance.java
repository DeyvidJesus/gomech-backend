package com.gomech.dto.Analytics;

public record PartStockBalance(
        Long partId,
        String partName,
        Long quantityPurchased,
        Long quantityUsed
) {
    public long unusedQuantity() {
        long purchased = quantityPurchased != null ? quantityPurchased : 0L;
        long used = quantityUsed != null ? quantityUsed : 0L;
        return purchased - used;
    }
}
