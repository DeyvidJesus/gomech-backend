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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
        AuditEventRequest request = new AuditEventRequest(
                "SERVICE_ORDER_UPDATED",
                "Cadastro de cliente ID 34",
                "deyvid.gondim@leoleo.com",
                "clientes",
                "ADMIN",
                java.time.LocalDateTime.of(2025, 11, 5, 16, 23),
                "orderId=1",
                34L  // entityId
        );

        mockMvc.perform(post("/audit/event")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventHash").isNotEmpty())
                .andExpect(jsonPath("$.eventType").value("SERVICE_ORDER_UPDATED"))
                .andExpect(jsonPath("$.operation").value("Cadastro de cliente ID 34"))
                .andExpect(jsonPath("$.userEmail").value("deyvid.gondim@leoleo.com"))
                .andExpect(jsonPath("$.moduleName").value("clientes"));

        assertThat(auditEventRepository.count()).isEqualTo(1);

        verify(blockchainService).publishAuditEvent(any(), any(), any(), any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void listEventsReturnsPaginatedAuditTrail() throws Exception {
        AuditEventRequest request = new AuditEventRequest(
                "CLIENT_CREATED",
                "Cadastro de cliente ID 1",
                "user@test.com",
                "clientes",
                "ADMIN",
                java.time.LocalDateTime.now().minusDays(1),
                "{}",
                1L  // entityId
        );

        auditEventRepository.deleteAll();
        mockMvc.perform(post("/audit/event")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/audit/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].operation").value("Cadastro de cliente ID 1"));
    }
}
