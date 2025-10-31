package com.gomech.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gomech.configuration.SecurityConfig;
import com.gomech.configuration.SecurityFilter;
import com.gomech.dto.Inventory.InventoryEntryRequestDTO;
import com.gomech.dto.Inventory.InventoryItemCreateDTO;
import com.gomech.dto.Inventory.InventoryItemResponseDTO;
import com.gomech.dto.Inventory.InventoryMovementResponseDTO;
import com.gomech.dto.Inventory.StockReservationRequestDTO;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import com.gomech.service.InventoryService;
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

@WebMvcTest(controllers = InventoryController.class)
@Import(SecurityConfig.class)
class InventoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private InventoryService inventoryService;

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
    @WithMockUser(roles = "USER")
    void shouldListInventoryItemsForAuthorizedUsers() throws Exception {
        InventoryItemResponseDTO response = new InventoryItemResponseDTO(1L, 2L, "Filtro", "FLT-001", "MAIN",
                10, 2, 1, new BigDecimal("30.00"), new BigDecimal("55.00"),
                LocalDateTime.now(), LocalDateTime.now());
        when(inventoryService.listItems(null)).thenReturn(List.of(response));

        mockMvc.perform(get("/inventory/items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].partSku").value("FLT-001"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldCreateInventoryItemWhenAdmin() throws Exception {
        InventoryItemCreateDTO request = new InventoryItemCreateDTO(2L, "MAIN", 1, new BigDecimal("30.00"), new BigDecimal("55.00"));
        InventoryItemResponseDTO response = new InventoryItemResponseDTO(1L, 2L, "Filtro", "FLT-001", "MAIN",
                0, 0, 1, new BigDecimal("30.00"), new BigDecimal("55.00"), LocalDateTime.now(), LocalDateTime.now());

        when(inventoryService.createItem(any(InventoryItemCreateDTO.class))).thenReturn(response);

        mockMvc.perform(post("/inventory/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.location").value("MAIN"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void shouldForbidInventoryCreationForNonAdmin() throws Exception {
        InventoryItemCreateDTO request = new InventoryItemCreateDTO(2L, "MAIN", 1, null, null);

        mockMvc.perform(post("/inventory/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldRegisterInventoryEntry() throws Exception {
        InventoryEntryRequestDTO request = new InventoryEntryRequestDTO(2L, "MAIN", 5, new BigDecimal("25.00"),
                new BigDecimal("45.00"), "NF-123", "Entrada manual");
        InventoryMovementResponseDTO response = new InventoryMovementResponseDTO(10L, 1L, 2L, "Filtro",
                null, 5, "NF-123", "Entrada manual", null, null, LocalDateTime.now());

        when(inventoryService.registerEntry(any(InventoryEntryRequestDTO.class))).thenReturn(response);

        mockMvc.perform(post("/inventory/movements/entry")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.quantity").value(5));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldReserveStock() throws Exception {
        StockReservationRequestDTO request = new StockReservationRequestDTO(5L, 2, "Reserva teste");
        InventoryMovementResponseDTO response = new InventoryMovementResponseDTO(11L, 1L, 2L, "Filtro",
                null, 2, null, "Reserva teste", 9L, 3L, LocalDateTime.now());

        when(inventoryService.reserveStock(any(StockReservationRequestDTO.class))).thenReturn(response);

        mockMvc.perform(post("/inventory/movements/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(11L));
    }
}
