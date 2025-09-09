package com.gomech.dto.Vehicles;

import com.gomech.model.Vehicle;

import java.util.Date;

public record VehicleResponseDTO(
        Long id,
        String licensePlate,
        String brand,
        String model,
        Date manufactureDate,
        String color,
        String observations,
        Integer kilometers,
        String chassisId,
        Long clientId
) {
    public static VehicleResponseDTO fromEntity(Vehicle vehicle) {
        return new VehicleResponseDTO(
                vehicle.getId(),
                vehicle.getLicensePlate(),
                vehicle.getBrand(),
                vehicle.getModel(),
                vehicle.getManufactureDate(),
                vehicle.getColor(),
                vehicle.getObservations(),
                vehicle.getKilometers(),
                vehicle.getChassisId(),
                vehicle.getClient() != null ? vehicle.getClient().getId() : null
        );
    }
}
