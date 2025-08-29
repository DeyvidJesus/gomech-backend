package com.gomech.dto;

import com.gomech.model.ServiceOrderStatus;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class ServiceOrderResponseDTO {
    private Long id;
    private String orderNumber;
    private Long vehicleId;
    private String vehicleLicensePlate;
    private String vehicleModel;
    private String vehicleBrand;
    private Long clientId;
    private String clientName;
    private String clientPhone;
    private String description;
    private String problemDescription;
    private String diagnosis;
    private String solutionDescription;
    private ServiceOrderStatus status;
    private BigDecimal laborCost;
    private BigDecimal partsCost;
    private BigDecimal totalCost;
    private BigDecimal discount;
    private BigDecimal finalCost;
    private LocalDateTime estimatedCompletion;
    private LocalDateTime actualCompletion;
    private String observations;
    private String technicianName;
    private BigDecimal currentKilometers;
    private List<ServiceOrderItemResponseDTO> items;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
