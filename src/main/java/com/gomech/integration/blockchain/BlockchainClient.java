package com.gomech.integration.blockchain;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "blockchainClient", url = "${blockchain.service.url}", dismiss404 = true)
public interface BlockchainClient {

    @PostMapping("/audit/events")
    BlockchainResponse publishAuditEvent(@RequestBody BlockchainRequest request);
}
