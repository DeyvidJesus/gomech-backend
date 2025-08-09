package com.gomech.dto;

public record LoginResponseDTO(String token, String email, String name, String role) {
}
