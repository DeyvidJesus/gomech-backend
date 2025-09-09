package com.gomech.dto.Vehicles;

import java.util.Date;

public record VehicleCreateDTO(
        String licensePlate,
        String brand,
        String model,
        Date manufactureDate,
        String color,
        String observations,
        Integer kilometers,
        String chassisId,
        Long clientId
) {}
