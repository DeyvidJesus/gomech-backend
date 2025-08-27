package com.gomech.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Setter
@Getter
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

    private String vehicleId;

    @ManyToOne
    @JoinColumn(name = "client_id")
    @JsonIgnore
    private Client client;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    public Vehicle() {}

    public Vehicle(String licensePlate, String brand, String model, Date manufactureDate, String color, String observations, float kilometers, String vehicleId) {
        this.licensePlate = licensePlate;
        this.brand = brand;
        this.model = model;
        this.manufactureDate = manufactureDate;
        this.color = color;
        this.observations = observations;
        this.kilometers = kilometers;
        this.vehicleId = vehicleId;
    }

}
