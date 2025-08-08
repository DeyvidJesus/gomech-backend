package com.gomech.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.List;

@Setter
@Getter
@Entity
@Table(name = "clients")
public class Client {
    private String name;

    private String document;

    private String phone;

    private String email;

    private String address;

    private Date birthDate;

    private String observations;

    private Date registrationDate = new Date();

    @OneToMany(mappedBy = "client", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<Vehicle> vehicles;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    public Client() {}

    public Client(String name, String document, String phone, String email, String address, String observations) {
        this.name = name;
        this.document = document;
        this.phone = phone;
        this.email = email;
        this.address = address;
        this.observations = observations;
    }

    public Client(String name, String document, String phone, String email, String address, String observations, Date birthDate) {
        this.name = name;
        this.document = document;
        this.phone = phone;
        this.email = email;
        this.address = address;
        this.birthDate = birthDate;
        this.observations = observations;
    }

}
