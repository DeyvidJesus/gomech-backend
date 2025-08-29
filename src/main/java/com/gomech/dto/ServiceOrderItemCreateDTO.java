package com.gomech.dto;

import com.gomech.model.ServiceOrderItemType;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class ServiceOrderItemCreateDTO {
    private String description;
    private ServiceOrderItemType itemType;
    private Integer quantity = 1;
    private BigDecimal unitPrice = BigDecimal.ZERO;
    private String productCode;
    private Boolean requiresStock = false;
    private String observations;
}
