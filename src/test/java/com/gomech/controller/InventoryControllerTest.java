package com.gomech.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gomech.configuration.SecurityConfig;
import com.gomech.configuration.SecurityFilter;
import com.gomech.dto.Inventory.CriticalPartReportDTO;
import com.gomech.dto.Inventory.InventoryEntryRequestDTO;
import com.gomech.dto.Inventory.InventoryItemCreateDTO;
import com.gomech.dto.Inventory.InventoryItemResponseDTO;
import com.gomech.dto.Inventory.InventoryMovementResponseDTO;
import com.gomech.dto.Inventory.InventoryRecommendationDTO;
import com.gomech.dto.Inventory.PartAvailabilityDTO;
import com.gomech.dto.Inventory.PartConsumptionStats;
import com.gomech.dto.Inventory.StockReservationRequestDTO;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import com.gomech.service.InventoryRecommendationService;
import com.gomech.service.InventoryReportService;
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
    private InventoryRecommendationService inventoryRecommendationService;

    @MockBean
    private InventoryReportService inventoryReportService;

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
        InventoryItemCreateDTO request = new InventoryItemCreateDTO(2L, "MAIN", 10, new BigDecimal("30.00"), new BigDecimal("55.00"));
        InventoryItemResponseDTO response = new InventoryItemResponseDTO(1L, 2L, "Filtro", "FLT-001", "MAIN",
                10, 0, 1, new BigDecimal("30.00"), new BigDecimal("55.00"), LocalDateTime.now(), LocalDateTime.now());

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
        InventoryItemCreateDTO request = new InventoryItemCreateDTO(2L, "MAIN", 5, null, null);

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

    @Test
    @WithMockUser(roles = "USER")
    void shouldReturnInventoryRecommendations() throws Exception {
        InventoryRecommendationDTO recommendation = new InventoryRecommendationDTO(1L, "Filtro", "FLT-001", 0.87,
                "Sugestão personalizada", false, 12L, LocalDateTime.now());

        when(inventoryRecommendationService.getRecommendations(any())).thenReturn(List.of(recommendation));

        mockMvc.perform(get("/inventory/recommendations").param("vehicleId", "200").param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].partSku").value("FLT-001"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void shouldExposeAiPipelines() throws Exception {
        when(inventoryRecommendationService.listAvailablePipelines()).thenReturn(List.of("inventory-recommendation"));

        mockMvc.perform(get("/inventory/recommendations/pipelines"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("inventory-recommendation"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void shouldExposeCriticalPartsReport() throws Exception {
        CriticalPartReportDTO report = new CriticalPartReportDTO(1L, "Filtro", "FLT-001", "Sedan",
                10L, 8L, 5L, 2L, 42L, LocalDateTime.now());
        when(inventoryReportService.listCriticalParts(null)).thenReturn(List.of(report));

        mockMvc.perform(get("/inventory/reports/critical-parts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].partSku").value("FLT-001"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void shouldReturnPartAvailability() throws Exception {
        PartAvailabilityDTO availability = new PartAvailabilityDTO(2L, "Pastilha", "PST-010",
                5L, 2L, 3L, 3L, LocalDateTime.now());
        when(inventoryReportService.getAvailabilityForPart(2L)).thenReturn(availability);

        mockMvc.perform(get("/inventory/availability/parts/{partId}", 2L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableQuantity").value(3));
    }

    @Test
    @WithMockUser(roles = "USER")
    void shouldListVehicleAvailability() throws Exception {
        PartAvailabilityDTO availability = new PartAvailabilityDTO(2L, "Pastilha", "PST-010",
                5L, 1L, 2L, 4L, LocalDateTime.now());
        when(inventoryReportService.listAvailabilityForVehicle(9L)).thenReturn(List.of(availability));

        mockMvc.perform(get("/inventory/availability/vehicles/{vehicleId}", 9L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].partName").value("Pastilha"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void shouldListClientAvailability() throws Exception {
        PartAvailabilityDTO availability = new PartAvailabilityDTO(2L, "Pastilha", "PST-010",
                5L, 1L, 2L, 4L, LocalDateTime.now());
        when(inventoryReportService.listAvailabilityForClient(11L)).thenReturn(List.of(availability));

        mockMvc.perform(get("/inventory/availability/clients/{clientId}", 11L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].availableQuantity").value(4));
    }

    @Test
    @WithMockUser(roles = "USER")
    void shouldReturnVehicleHistory() throws Exception {
        PartConsumptionStats stats = new PartConsumptionStats(2L, "Filtro", "FLT-001",
                8L, 3L, 2L, LocalDateTime.now());
        when(inventoryReportService.getVehicleConsumptionHistory(7L)).thenReturn(List.of(stats));

        mockMvc.perform(get("/inventory/history/vehicles/{vehicleId}", 7L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].totalQuantity").value(8));
    }

    @Test
    @WithMockUser(roles = "USER")
    void shouldReturnClientHistory() throws Exception {
        PartConsumptionStats stats = new PartConsumptionStats(2L, "Filtro", "FLT-001",
                8L, 3L, 2L, LocalDateTime.now());
        when(inventoryReportService.getClientConsumptionHistory(15L)).thenReturn(List.of(stats));

        mockMvc.perform(get("/inventory/history/clients/{clientId}", 15L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].distinctVehicles").value(2));
    }
}
