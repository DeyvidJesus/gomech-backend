package com.gomech.service;

import com.gomech.integration.blockchain.BlockchainClient;
import com.gomech.integration.blockchain.BlockchainRequest;
import com.gomech.integration.blockchain.BlockchainResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.format.DateTimeFormatter;

@Service
public class BlockchainService {

    private static final Logger LOGGER = LoggerFactory.getLogger(BlockchainService.class);
    private final BlockchainClient blockchainClient;

    public BlockchainService(BlockchainClient blockchainClient) {
        this.blockchainClient = blockchainClient;
    }

    public String publishAuditEvent(String eventType, String hash, String payload, Instant timestamp) {
        try {
            BlockchainResponse response = blockchainClient.publishAuditEvent(new BlockchainRequest(eventType, hash, payload,
                    DateTimeFormatter.ISO_INSTANT.format(timestamp)));
            return response != null ? response.transactionHash() : null;
        } catch (Exception e) {
            LOGGER.warn("Failed to publish audit event to blockchain: {}", e.getMessage());
            return null;
        }
    }
}
