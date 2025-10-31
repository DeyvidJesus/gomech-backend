package com.gomech.model;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter
@ToString(exclude = {"part"})
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "inventory_movements")
public class InventoryMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "part_id", nullable = false)
    private Part part;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InventoryMovementType type;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "reserved_change", nullable = false)
    private Integer reservedChange = 0;

    @Column(length = 255)
    private String description;

    @Column(name = "service_order_id")
    private Long serviceOrderId;

    @Column(name = "service_order_item_id")
    private Long serviceOrderItemId;

    @Column(name = "reference_code", length = 100)
    private String referenceCode;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
