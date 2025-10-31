package com.gomech.integration.blockchain;

public record BlockchainRequest(String eventType, String eventHash, String payload, String timestamp) {
}
