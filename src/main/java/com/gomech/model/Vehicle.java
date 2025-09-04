package com.gomech.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.Date;

@Data
@Entity
@Table(name = "vehicles")
public class Vehicle {
    private String licensePlate;

    private String brand;

    private String model;

    private Date manufactureDate;

    private String color;

    private String observations;

    private float kilometers;

    private String chassisId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")
    @JsonIgnore
    private Client client;

    @Transient
    private Long clientId;

    @CreationTimestamp
    @Column(name = "createdAt", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updatedAt")
    private LocalDateTime updatedAt;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    public Vehicle() {}

    public Vehicle(String licensePlate, String brand, String model, Date manufactureDate, String color, String observations, float kilometers, String chassisId) {
        this.licensePlate = licensePlate;
        this.brand = brand;
        this.model = model;
        this.manufactureDate = manufactureDate;
        this.color = color;
        this.observations = observations;
        this.kilometers = kilometers;
        this.chassisId = chassisId;
    }

    // Métodos personalizados para clientId
    public Long getClientId() {
        return this.client != null ? this.client.getId() : this.clientId;
    }

    public void setClientId(Long clientId) {
        this.clientId = clientId;
    }
}
