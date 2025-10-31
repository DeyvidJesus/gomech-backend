package com.gomech.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gomech.configuration.SecurityConfig;
import com.gomech.configuration.SecurityFilter;
import com.gomech.dto.Parts.PartCreateDTO;
import com.gomech.dto.Parts.PartResponseDTO;
import com.gomech.service.PartService;
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

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PartController.class)
@Import(SecurityConfig.class)
class PartControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PartService partService;

    @MockBean
    private SecurityFilter securityFilter;

    @BeforeEach
    void setUpSecurityFilter() throws ServletException, IOException {
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
    void shouldCreatePartWhenAdmin() throws Exception {
        PartCreateDTO request = new PartCreateDTO("Filtro de Óleo", "FLT-001", "ACME", "Filtro premium",
                new BigDecimal("30.00"), new BigDecimal("55.00"), true);
        PartResponseDTO response = new PartResponseDTO(1L, request.name(), request.sku(), request.manufacturer(),
                request.description(), request.unitCost(), request.unitPrice(), request.active(),
                LocalDateTime.now(), LocalDateTime.now());

        when(partService.register(any(PartCreateDTO.class))).thenReturn(response);

        mockMvc.perform(post("/parts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("Filtro de Óleo"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void shouldForbidPartCreationForNonAdmin() throws Exception {
        PartCreateDTO request = new PartCreateDTO("Pastilha", "PST-01", null, null,
                new BigDecimal("10.00"), new BigDecimal("15.00"), true);

        mockMvc.perform(post("/parts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "USER")
    void shouldListPartsForAuthorizedUsers() throws Exception {
        PartResponseDTO response = new PartResponseDTO(1L, "Filtro", "FLT-001", "ACME", "Desc",
                new BigDecimal("30.00"), new BigDecimal("55.00"), true,
                LocalDateTime.now(), LocalDateTime.now());

        when(partService.listAll()).thenReturn(List.of(response));

        mockMvc.perform(get("/parts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].sku").value("FLT-001"));
    }
}
