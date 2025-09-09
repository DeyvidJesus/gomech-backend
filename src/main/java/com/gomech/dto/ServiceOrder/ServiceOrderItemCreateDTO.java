package com.gomech.dto.ServiceOrder;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.gomech.model.ServiceOrderItemType;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ServiceOrderItemCreateDTO {
    private String description;
    private ServiceOrderItemType itemType;
    private Integer quantity;
    private BigDecimal unitPrice;
    private String productCode;
    private Long stockProductId;
    private Boolean requiresStock;
    private String observations;
}
