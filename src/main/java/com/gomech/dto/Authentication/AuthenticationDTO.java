package com.gomech.dto.Authentication;

public record AuthenticationDTO(String email, String password, String mfaCode) {
}
