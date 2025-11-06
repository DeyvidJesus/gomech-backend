package com.gomech.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gomech.configuration.SecurityConfig;
import com.gomech.configuration.SecurityFilter;
import com.gomech.dto.Analytics.AnalyticsInsightDTO;
import com.gomech.dto.Analytics.AnalyticsRequestDTO;
import com.gomech.service.AnalyticsInsightService;
import com.gomech.service.AnalyticsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AnalyticsController.class)
@Import(SecurityConfig.class)
class AnalyticsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AnalyticsService analyticsService;

    @MockBean
    private AnalyticsInsightService analyticsInsightService;

    @MockBean
    private SecurityFilter securityFilter;

    @BeforeEach
    void configureSecurityFilter() throws ServletException, java.io.IOException {
        doAnswer(invocation -> {
            HttpServletRequest request = invocation.getArgument(0);
            HttpServletResponse response = invocation.getArgument(1);
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(request, response);
            return null;
        }).when(securityFilter).doFilter(any(HttpServletRequest.class), any(HttpServletResponse.class), any(FilterChain.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getInsightsReturnsHighlights() throws Exception {
        when(analyticsInsightService.generateInsights()).thenReturn(List.of(
                new AnalyticsInsightDTO("Peça destaque", "Peça X foi a que mais saiu.", "INVENTORY")
        ));

        mockMvc.perform(get("/analytics/insights"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Peça destaque"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void postAnalyticsDelegatesToExternalService() throws Exception {
        when(analyticsService.requestAnalytics(any())).thenReturn(new com.gomech.integration.analytics.AnalyticsResponse(
                "SUCCESS",
                Map.of("insight", "ok")
        ));

        AnalyticsRequestDTO requestDTO = new AnalyticsRequestDTO("metric", Map.of("key", "value"));

        mockMvc.perform(post("/analytics")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));
    }
}
