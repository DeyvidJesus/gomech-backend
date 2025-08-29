package com.gomech.dto;

import com.gomech.model.ServiceOrderItemType;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class ServiceOrderItemResponseDTO {
    private Long id;
    private String description;
    private ServiceOrderItemType itemType;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;
    private String productCode;
    private Boolean requiresStock;
    private Boolean stockReserved;
    private Boolean applied;
    private String observations;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
