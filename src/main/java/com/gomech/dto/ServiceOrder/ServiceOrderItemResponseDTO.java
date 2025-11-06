package com.gomech.dto.ServiceOrder;

import com.gomech.model.ServiceOrderItemType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ServiceOrderItemResponseDTO {
    private Long id;
    private String description;
    private ServiceOrderItemType itemType;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;
    private String productCode;
    private Long partId;
    private String partName;
    private String partSku;
    private Long inventoryItemId;
    private String inventoryLocation;
    private Boolean requiresStock;
    private Boolean stockReserved;
    private Boolean applied;
    private String observations;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
