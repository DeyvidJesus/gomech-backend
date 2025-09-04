package com.gomech.dto;

import com.gomech.model.ServiceOrderItemType;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ServiceOrderItemCreateDTO {
    private String description;
    private ServiceOrderItemType itemType;
    private Integer quantity = 1;
    private BigDecimal unitPrice = BigDecimal.ZERO;
    private String productCode;
    private Boolean requiresStock = false;
    private String observations;
}
