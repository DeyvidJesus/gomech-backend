package com.gomech.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.Date;

@Data
@Entity
@Setter
@Getter
@Table(name = "vehicles")
public class Vehicle {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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
