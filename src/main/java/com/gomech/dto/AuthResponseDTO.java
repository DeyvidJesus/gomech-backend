package com.gomech.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AuthResponseDTO {

    private String token;
    private String type = "Bearer";
    private String email;
    private Long userId;
    private String roleName;

    public AuthResponseDTO(String token, String email, Long userId, String roleName) {
        this.token = token;
        this.email = email;
        this.userId = userId;
        this.roleName = roleName;
    }
} 