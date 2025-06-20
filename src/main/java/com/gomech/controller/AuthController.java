package com.gomech.controller;

import com.gomech.dto.AuthenticationDTO;
import com.gomech.dto.TokenDTO;
import com.gomech.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    @Autowired
    private AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<TokenDTO> login(@RequestBody AuthenticationDTO data) {
        String token = authService.login(data.getEmail(), data.getPassword());
        return ResponseEntity.ok(new TokenDTO(token));
    }
}
