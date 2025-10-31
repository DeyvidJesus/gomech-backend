package com.gomech.dto.Authentication;

public record LoginResponseDTO(String accessToken, String refreshToken, boolean mfaRequired, String email, String name, String role, Long id) {
}
