package com.gomech.model;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "parts")
public class Part {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String code;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "unit_cost", precision = 10, scale = 2)
    private BigDecimal unitCost = BigDecimal.ZERO;

    @Column(nullable = false)
    private Integer quantity = 0;

    @Column(name = "reserved_quantity", nullable = false)
    private Integer reservedQuantity = 0;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public int getAvailableQuantity() {
        return (quantity != null ? quantity : 0) - (reservedQuantity != null ? reservedQuantity : 0);
    }

    public void increaseQuantity(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Quantidade para incremento deve ser positiva");
        }
        this.quantity = (this.quantity != null ? this.quantity : 0) + amount;
    }

    public void decreaseQuantity(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Quantidade para redução deve ser positiva");
        }
        int current = this.quantity != null ? this.quantity : 0;
        if (current < amount) {
            throw new IllegalStateException("Estoque insuficiente para realizar a baixa");
        }
        this.quantity = current - amount;
    }

    public void reserve(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Quantidade para reserva deve ser positiva");
        }
        int available = getAvailableQuantity();
        if (available < amount) {
            throw new IllegalStateException("Quantidade insuficiente para reserva");
        }
        this.reservedQuantity = (this.reservedQuantity != null ? this.reservedQuantity : 0) + amount;
    }

    public void releaseReservation(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Quantidade para liberar deve ser positiva");
        }
        int current = this.reservedQuantity != null ? this.reservedQuantity : 0;
        if (current < amount) {
            throw new IllegalStateException("Quantidade reservada insuficiente para liberação");
        }
        this.reservedQuantity = current - amount;
    }
}
