package com.gomech.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TokenDTO {
    private String token;
    private String email;
    private String role;
    private Long userId;
    
    public TokenDTO(String token) {
        this.token = token;
    }
}
