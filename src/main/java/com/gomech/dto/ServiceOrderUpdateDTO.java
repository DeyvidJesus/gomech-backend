package com.gomech.dto;

import com.gomech.model.ServiceOrderStatus;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
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
    private LocalDateTime actualCompletion;
    private String observations;
    private String technicianName;
    private BigDecimal currentKilometers;
}
