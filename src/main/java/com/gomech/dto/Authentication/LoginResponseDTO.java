package com.gomech.dto.Authentication;

public record LoginResponseDTO(String token, String email, String name, String role) {
}
