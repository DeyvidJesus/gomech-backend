package com.gomech.controller;

import com.gomech.dto.AuthenticationDTO;
import com.gomech.dto.TokenDTO;
import com.gomech.dto.RegisterDTO;
import com.gomech.model.User;
import com.gomech.service.AuthService;
import org.springframework.http.HttpStatus;
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
        TokenDTO tokenResponse = authService.login(data.getEmail(), data.getPassword());
        return ResponseEntity.ok(tokenResponse);
    }

    @PostMapping("/register")
    public ResponseEntity<User> register(@RequestBody RegisterDTO dto) {
        User user = authService.createUser(dto.getEmail(), dto.getPassword(), dto.getRoleId());
        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }
}
