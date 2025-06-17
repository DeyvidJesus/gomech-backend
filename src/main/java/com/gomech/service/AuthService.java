package com.gomech.service;

import com.gomech.dto.AuthResponseDTO;
import com.gomech.dto.LoginRequestDTO;
import com.gomech.dto.RegisterRequestDTO;
import com.gomech.model.Role;
import com.gomech.model.User;
import com.gomech.repository.UserRepository;
import com.gomech.repository.RoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private AuthenticationManager authenticationManager;

    public AuthResponseDTO login(LoginRequestDTO loginRequest) {
        try {
            // Autenticar usuário
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    loginRequest.getEmail(),
                    loginRequest.getPassword()
                )
            );

            User user = (User) authentication.getPrincipal();
            String token = tokenService.generateToken(user);

            return new AuthResponseDTO(
                token,
                user.getEmail(),
                user.getId(),
                user.getRole().getNome()
            );
        } catch (Exception e) {
            throw new BadCredentialsException("Credenciais inválidas");
        }
    }

    public AuthResponseDTO register(RegisterRequestDTO registerRequest) {
        // Verificar se o usuário já existe
        if (userRepository.findByEmail(registerRequest.getEmail()).isPresent()) {
            throw new RuntimeException("Email já está em uso");
        }

        // Buscar role
        Role role;
        if (registerRequest.getRoleId() != null) {
            role = roleRepository.findById(registerRequest.getRoleId())
                .orElseThrow(() -> new RuntimeException("Role não encontrada"));
        } else {
            // Role padrão (usuário comum)
            role = roleRepository.findByNome("USER")
                .orElseThrow(() -> new RuntimeException("Role padrão não encontrada"));
        }

        // Criar novo usuário
        User newUser = new User();
        newUser.setEmail(registerRequest.getEmail());
        newUser.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        newUser.setRole(role);

        User savedUser = userRepository.save(newUser);
        String token = tokenService.generateToken(savedUser);

        return new AuthResponseDTO(
            token,
            savedUser.getEmail(),
            savedUser.getId(),
            savedUser.getRole().getNome()
        );
    }

    public boolean validateToken(String authHeader) {
        try {
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                return tokenService.validateToken(token);
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }
} 