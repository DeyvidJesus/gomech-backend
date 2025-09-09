package com.gomech.dto.ServiceOrder;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ServiceOrderCreateDTO {
    private Long vehicleId;
    private Long clientId;
    private String description;
    private String problemDescription;
    private String technicianName;
    private BigDecimal currentKilometers;
    private LocalDateTime estimatedCompletion;
    private String observations;
    private BigDecimal laborCost;
    private BigDecimal partsCost;
    private BigDecimal discount;
    private List<ServiceOrderItemCreateDTO> items;
}
