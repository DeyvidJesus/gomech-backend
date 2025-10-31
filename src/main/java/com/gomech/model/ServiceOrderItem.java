package com.gomech.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@ToString(exclude = {"serviceOrder"})
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "service_items")
public class ServiceOrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
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

    @Column(precision = 10, scale = 2, nullable = false,
            columnDefinition = "numeric(10,2) generated always as (quantity * unit_price) stored",
            insertable = false, updatable = false)
    private BigDecimal totalPrice;

    @Column(length = 100)
    private String productCode;

    @Column(name = "stock_product_id")
    private Long stockProductId;

    @Column(nullable = false)
    private Boolean requiresStock = false;

    @Column(nullable = false)
    private Boolean stockReserved = false;

    @Column(columnDefinition = "TEXT")
    private String observations;

    @Column(nullable = false)
    private Boolean applied = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // totalPrice é calculado automaticamente pelo banco de dados como campo generated

    public void apply() { this.applied = true; }
    public void unapply() { this.applied = false; }
}
