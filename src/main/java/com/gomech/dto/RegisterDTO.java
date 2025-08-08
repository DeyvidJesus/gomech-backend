package com.gomech.dto;

import com.gomech.model.Role;

public record RegisterDTO(String email, String password, Role role) {
}