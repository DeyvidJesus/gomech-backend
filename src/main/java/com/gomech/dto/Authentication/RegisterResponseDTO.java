package com.gomech.dto.Authentication;

public record RegisterResponseDTO(Long userId, boolean mfaEnabled, String mfaSecret) {
}
