package com.gomech.dto.Clients;

import com.gomech.dto.Vehicles.VehicleResponseDTO;
import com.gomech.model.Client;

import java.util.Date;
import java.util.List;
import java.util.Optional;

public record ClientResponseDTO(
        Long id,
        String name,
        String document,
        String phone,
        String email,
        String address,
        Date birthDate,
        String observations,
        List<VehicleResponseDTO> vehicles
) {
    public static ClientResponseDTO fromEntity(Client client) {
        return new ClientResponseDTO(
                client.getId(),
                client.getName(),
                client.getDocument(),
                client.getPhone(),
                client.getEmail(),
                client.getAddress(),
                client.getBirthDate(),
                client.getObservations(),
                client.getVehicles() != null
                        ? client.getVehicles().stream()
                        .map(VehicleResponseDTO::fromEntity)
                        .toList()
                        : null
        );
    }
}
