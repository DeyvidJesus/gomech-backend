package com.gomech.controller;

import com.gomech.dto.AuthenticationDTO;
import com.gomech.dto.TokenDTO;
import com.gomech.dto.RegisterDTO;
import com.gomech.model.User;
import com.gomech.service.AuthService;
import com.gomech.service.UserService;
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

    @Autowired
    private UserService userService;

    @PostMapping("/login")
    public ResponseEntity<TokenDTO> login(@RequestBody AuthenticationDTO data) {
        String token = authService.login(data.getEmail(), data.getPassword());
        return ResponseEntity.ok(new TokenDTO(token));
    }

    @PostMapping("/register")
    public ResponseEntity<User> register(@RequestBody RegisterDTO dto) {
        User user = userService.createUser(dto.getEmail(), dto.getPassword(), dto.getRoleId());
        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }
}
