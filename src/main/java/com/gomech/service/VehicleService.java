package com.gomech.service;

import com.gomech.model.Vehicle;
import com.gomech.repository.VehicleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class VehicleService {
    @Autowired
    private VehicleRepository repository;

    public Vehicle save(Vehicle vehicle) {
        return repository.save(vehicle);
    }

    public List<Vehicle> listAll() {
        return repository.findAll();
    }


    public Optional<Vehicle> getById(Long id) {
        return repository.findById(id);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }

    public Vehicle update(Long id, Vehicle updatedVehicle) {
        Vehicle vehicle = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Vehicle not found"));
        vehicle.setLicensePlate(updatedVehicle.getLicensePlate());
        vehicle.setBrand(updatedVehicle.getBrand());
        vehicle.setModel(updatedVehicle.getModel());
        vehicle.setColor(updatedVehicle.getColor());
        vehicle.setManufactureDate((updatedVehicle.getManufactureDate()));
        return repository.save(vehicle);
    }
}
