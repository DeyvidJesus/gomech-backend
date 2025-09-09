package com.gomech.dto.ServiceOrder;

import com.gomech.model.ServiceOrderStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ServiceOrderUpdateDTO {
    private String description;
    private String problemDescription;
    private String diagnosis;
    private String solutionDescription;
    private ServiceOrderStatus status;
    private BigDecimal laborCost;
    private BigDecimal partsCost;
    private BigDecimal discount;
    private LocalDateTime estimatedCompletion;
    private String observations;
    private String technicianName;
    private BigDecimal currentKilometers;
}
