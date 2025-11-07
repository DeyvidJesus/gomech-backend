package com.gomech.model;

import com.gomech.domain.InventoryMovement;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
@Entity
@Setter
@Getter
@ToString(exclude = "inventoryMovements")
@EqualsAndHashCode(exclude = "inventoryMovements")
@Table(name = "vehicles")
@EntityListeners(com.gomech.listener.OrganizationEntityListener.class)
public class Vehicle {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @Column(nullable = false, length = 10)
    private String licensePlate;

    @Column(nullable = false, length = 50)
    private String brand;

    @Column(nullable = false, length = 50)
    private String model;

    @Column(nullable = false)
    private Date manufactureDate;

    @Column(length = 30)
    private String color;

    @Column(columnDefinition = "TEXT")
    private String observations;

    @Column(nullable = false)
    private Integer kilometers;

    @Column(nullable = false, length = 50)
    private String chassisId;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "vehicle", cascade = CascadeType.ALL, orphanRemoval = false, fetch = FetchType.LAZY)
    private List<InventoryMovement> inventoryMovements = new ArrayList<>();

    public Vehicle() {}

    public Vehicle(String licensePlate, String brand, String model, Date manufactureDate, String color, String observations, float kilometers, String chassisId) {
        this.licensePlate = licensePlate;
        this.brand = brand;
        this.model = model;
        this.manufactureDate = manufactureDate;
        this.color = color;
        this.observations = observations;
        this.kilometers = (int) kilometers;
        this.chassisId = chassisId;
    }
}
