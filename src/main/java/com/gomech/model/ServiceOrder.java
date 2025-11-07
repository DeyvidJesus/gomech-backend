package com.gomech.model;

import com.gomech.domain.InventoryMovement;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ToString(exclude = {"items", "vehicle", "client", "inventoryMovements"})
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "service_orders")
@EntityListeners(com.gomech.listener.OrganizationEntityListener.class)
public class ServiceOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(unique = true, nullable = false, length = 50)
    private String orderNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

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

    @OneToMany(mappedBy = "serviceOrder", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<InventoryMovement> inventoryMovements = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public ServiceOrder() {
        this.generateOrderNumber();
    }

    private void generateOrderNumber() {
        this.orderNumber = "OS-" + LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
    }

    public void calculateTotalCost() {
        // Considera apenas itens aplicados para o cálculo
        BigDecimal itemsTotal = items.stream()
                .filter(item -> item.getApplied() != null && item.getApplied()) // Apenas itens aplicados
                .map(ServiceOrderItem::getTotalPrice)
                .filter(price -> price != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal safeLaborCost = this.laborCost != null ? this.laborCost : BigDecimal.ZERO;
        BigDecimal safePartsCost = this.partsCost != null ? this.partsCost : BigDecimal.ZERO;
        BigDecimal safeDiscount = this.discount != null ? this.discount : BigDecimal.ZERO;

        this.totalCost = safeLaborCost.add(safePartsCost).add(itemsTotal);
    }

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

    public void addInventoryMovement(InventoryMovement movement) {
        movement.setServiceOrder(this);
        this.inventoryMovements.add(movement);
    }

    public void removeInventoryMovement(InventoryMovement movement) {
        this.inventoryMovements.remove(movement);
        movement.setServiceOrder(null);
    }
}
