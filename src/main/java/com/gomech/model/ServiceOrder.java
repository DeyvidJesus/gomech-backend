package com.gomech.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Table(name = "service_orders")
public class ServiceOrder {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 50)
    private String orderNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    // Campos para facilitar queries sem joins
    @Column(name = "vehicle_id", insertable = false, updatable = false)
    private Long vehicleId;

    @Column(name = "client_id", insertable = false, updatable = false)
    private Long clientId;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String problemDescription;

    @Column(columnDefinition = "TEXT")
    private String diagnosis;

    @Column(columnDefinition = "TEXT")
    private String solutionDescription;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ServiceOrderStatus status = ServiceOrderStatus.PENDING;

    @Column(precision = 10, scale = 2)
    private BigDecimal laborCost = BigDecimal.ZERO;
    
    @Column(precision = 10, scale = 2)
    private BigDecimal partsCost = BigDecimal.ZERO;
    
    @Column(precision = 10, scale = 2)
    private BigDecimal totalCost = BigDecimal.ZERO;

    @Column(precision = 10, scale = 2)
    private BigDecimal discount = BigDecimal.ZERO;

    @Column(precision = 10, scale = 2)
    private BigDecimal finalCost = BigDecimal.ZERO;

    private LocalDateTime estimatedCompletion;
    
    private LocalDateTime actualCompletion;

    @Column(columnDefinition = "TEXT")
    private String observations;

    @Column(length = 100)
    private String technicianName;

    @Column(precision = 10, scale = 2)
    private BigDecimal currentKilometers;

    @OneToMany(mappedBy = "serviceOrder", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ServiceOrderItem> items = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "createdAt", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updatedAt")
    private LocalDateTime updatedAt;

    public ServiceOrder() {
        this.generateOrderNumber();
    }

    private void generateOrderNumber() {
        // Gera número da OS com formato: OS-YYYYMMDD-HHMMSS
        this.orderNumber = "OS-" + java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
    }

    public void calculateTotalCost() {
        // Calcula o total dos itens
        BigDecimal itemsTotal = items != null ? 
            items.stream()
                .map(ServiceOrderItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add) : BigDecimal.ZERO;
        
        // Soma mão de obra + peças + itens
        this.totalCost = this.laborCost.add(this.partsCost).add(itemsTotal);
        
        // Aplica desconto
        this.finalCost = this.totalCost.subtract(this.discount);
    }

    // Métodos de conveniência para gerenciar itens
    public void addItem(ServiceOrderItem item) {
        item.setServiceOrder(this);
        this.items.add(item);
        calculateTotalCost();
    }

    public void removeItem(ServiceOrderItem item) {
        this.items.remove(item);
        item.setServiceOrder(null);
        calculateTotalCost();
    }

    // Métodos para verificar status
    public boolean isPending() {
        return this.status == ServiceOrderStatus.PENDING;
    }

    public boolean isInProgress() {
        return this.status == ServiceOrderStatus.IN_PROGRESS;
    }

    public boolean isCompleted() {
        return this.status == ServiceOrderStatus.COMPLETED;
    }

    public boolean isCancelled() {
        return this.status == ServiceOrderStatus.CANCELLED;
    }
}
