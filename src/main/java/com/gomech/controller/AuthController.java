package com.gomech.controller;

import com.gomech.configuration.TokenService;
import com.gomech.dto.Authentication.AuthenticationDTO;
import com.gomech.dto.Authentication.LoginResponseDTO;
import com.gomech.dto.Authentication.RefreshTokenRequest;
import com.gomech.dto.Authentication.RegisterDTO;
import com.gomech.dto.Authentication.RegisterResponseDTO;
import com.gomech.dto.Authentication.TokenPairDTO;
import com.gomech.model.RefreshToken;
import com.gomech.model.User;
import com.gomech.service.MfaService;
import com.gomech.service.RefreshTokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.gomech.repository.UserRepository;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/auth")
public class AuthController {
    @Autowired
    private AuthenticationManager authenticationManager;
    @Autowired
    private UserRepository repository;
    @Autowired
    private TokenService tokenService;
    @Autowired
    private RefreshTokenService refreshTokenService;
    @Autowired
    private MfaService mfaService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@RequestBody @Valid AuthenticationDTO data) {
        var usernamePassword = new UsernamePasswordAuthenticationToken(data.email(), data.password());
        var auth = this.authenticationManager.authenticate(usernamePassword);

        var user = (User) auth.getPrincipal();
        if (user.isMfaEnabled()) {
            if (data.mfaCode() == null || !mfaService.verifyCode(user.getMfaSecret(), data.mfaCode())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new LoginResponseDTO(null, null, true, user.getEmail(), user.getName(), user.getRole().name(), user.getId()));
            }
        }

        var accessToken = tokenService.generateAccessToken(user);
        var refreshToken = refreshTokenService.createToken(user);

        return ResponseEntity.ok(new LoginResponseDTO(accessToken, refreshToken, false, user.getEmail(), user.getName(), user.getRole().name(), user.getId()));
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterResponseDTO> register(@RequestBody @Valid RegisterDTO data){
        if(this.repository.findByEmail(data.email()) != null) return ResponseEntity.badRequest().build();

        String encryptedPassword = new BCryptPasswordEncoder().encode(data.password());
        User newUser = new User(data.name(), data.email(), encryptedPassword, data.role());

        String mfaSecret = null;
        if (data.mfaEnabled()) {
            String secret = mfaService.generateSecret();
            newUser.enableMfa(mfaService.encryptSecret(secret));
            mfaSecret = secret;
        }

        this.repository.save(newUser);

        return ResponseEntity.ok(new RegisterResponseDTO(newUser.getId(), data.mfaEnabled(), mfaSecret));
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenPairDTO> refresh(@RequestBody @Valid RefreshTokenRequest request) {
        return refreshTokenService.validate(request.refreshToken())
                .map(RefreshToken::getUser)
                .map(user -> {
                    String newAccessToken = tokenService.generateAccessToken(user);
                    String newRefreshToken = refreshTokenService.createToken(user);
                    return ResponseEntity.ok(new TokenPairDTO(newAccessToken, newRefreshToken));
                }).orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }
}
