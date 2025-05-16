package com.gomech.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;

import java.util.Date;

@Setter
@Getter
@Entity
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
    private Client client;

    @jakarta.persistence.Id
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
