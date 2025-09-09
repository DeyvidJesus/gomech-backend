package com.gomech.dto.Authentication;

import com.gomech.model.Role;

public record RegisterDTO(String name, String email, String password, Role role) {
}