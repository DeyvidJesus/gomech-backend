package com.gomech.dto.Clients;

import java.util.Date;

public record ClientUpdateDTO(
        String name,
        String phone,
        String email,
        String document,
        String address,
        Date birthDate,
        String observations
) {}
