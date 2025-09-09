package com.gomech.dto.ServiceOrder;

import com.gomech.model.ServiceOrderStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ServiceOrderResponseDTO {
    private Long id;
    private String orderNumber;
    private Long vehicleId;
    private Long clientId;
    private String vehicleLicensePlate;
    private String vehicleModel;
    private String vehicleBrand;
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
    private LocalDateTime estimatedCompletion;
    private LocalDateTime actualCompletion;
    private String observations;
    private String technicianName;
    private BigDecimal currentKilometers;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<ServiceOrderItemResponseDTO> items;
}
