package com.gomech.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "service_order_items")
public class ServiceOrderItem {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_order_id", nullable = false)
    @JsonIgnore
    private ServiceOrder serviceOrder;

    @Column(nullable = false, length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ServiceOrderItemType itemType;

    @Column(nullable = false)
    private Integer quantity = 1;

    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal unitPrice = BigDecimal.ZERO;

    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal totalPrice = BigDecimal.ZERO;

    // Campo para futuro controle de estoque
    @Column(length = 100)
    private String productCode;

    // Campo para indicar se o item requer controle de estoque
    @Column(nullable = false)
    private Boolean requiresStock = false;

    // Campo para indicar se o estoque foi reservado (para futuro uso)
    @Column(nullable = false)
    private Boolean stockReserved = false;

    // Campo para armazenar ID do produto no estoque (para futuro uso)
    private Long stockProductId;

    @Column(columnDefinition = "TEXT")
    private String observations;

    // Campo para indicar se o item foi entregue/aplicado
    @Column(nullable = false)
    private Boolean applied = false;

    @CreationTimestamp
    @Column(name = "createdAt", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updatedAt")
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    public void calculateTotalPrice() {
        if (unitPrice != null && quantity != null) {
            this.totalPrice = unitPrice.multiply(BigDecimal.valueOf(quantity));
        } else {
            this.totalPrice = BigDecimal.ZERO;
        }
    }

    // Métodos de conveniência
    public boolean isService() {
        return this.itemType == ServiceOrderItemType.SERVICE;
    }

    public boolean isPart() {
        return this.itemType == ServiceOrderItemType.PART;
    }

    public boolean isMaterial() {
        return this.itemType == ServiceOrderItemType.MATERIAL;
    }

    public boolean isLabor() {
        return this.itemType == ServiceOrderItemType.LABOR;
    }

    // Método para aplicar/entregar o item
    public void apply() {
        this.applied = true;
    }

    // Método para reverter aplicação
    public void unapply() {
        this.applied = false;
    }
}
