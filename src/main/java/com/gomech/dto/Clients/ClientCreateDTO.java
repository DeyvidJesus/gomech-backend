package com.gomech.dto.Clients;

import com.gomech.dto.Vehicles.VehicleCreateDTO;

import java.util.Date;
import java.util.List;

public record ClientCreateDTO(
        String name,
        String document,
        String phone,
        String email,
        String address,
        Date birthDate,
        String observations,
        List<VehicleCreateDTO> vehicles
) {}
