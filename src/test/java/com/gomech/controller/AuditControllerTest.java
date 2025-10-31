package com.gomech.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gomech.dto.Audit.AuditEventRequest;
import com.gomech.repository.AuditEventRepository;
import com.gomech.service.BlockchainService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuditControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuditEventRepository auditEventRepository;

    @MockBean
    private BlockchainService blockchainService;

    @BeforeEach
    void setup() {
        auditEventRepository.deleteAll();
        when(blockchainService.publishAuditEvent(any(), any(), any(), any())).thenReturn("0x123");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void auditEventPersistsAndPublishesToBlockchain() throws Exception {
        AuditEventRequest request = new AuditEventRequest("SERVICE_ORDER_UPDATED", "orderId=1");

        mockMvc.perform(post("/audit/event")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventHash").isNotEmpty())
                .andExpect(jsonPath("$.eventType").value("SERVICE_ORDER_UPDATED"));

        assertThat(auditEventRepository.count()).isEqualTo(1);

        verify(blockchainService).publishAuditEvent(any(), any(), any(), any());
    }
}
