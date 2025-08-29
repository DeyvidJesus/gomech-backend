package com.gomech.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class ServiceOrderCreateDTO {
    private Long vehicleId;
    private Long clientId;
    private String description;
    private String problemDescription;
    private String technicianName;
    private BigDecimal currentKilometers;
    private LocalDateTime estimatedCompletion;
    private String observations;
    private List<ServiceOrderItemCreateDTO> items;
}
