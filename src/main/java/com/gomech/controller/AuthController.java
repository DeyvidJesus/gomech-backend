package com.gomech.controller;

import com.gomech.configuration.TokenService;
import com.gomech.dto.Authentication.AuthenticationDTO;
import com.gomech.dto.Authentication.LoginResponseDTO;
import com.gomech.dto.Authentication.RefreshTokenRequest;
import com.gomech.dto.Authentication.RegisterDTO;
import com.gomech.dto.Authentication.RegisterResponseDTO;
import com.gomech.dto.Authentication.RegisterOrganizationDTO;
import com.gomech.dto.Authentication.RegisterOrganizationResponseDTO;
import com.gomech.dto.Authentication.TokenPairDTO;
import com.gomech.dto.Organization.OrganizationBasicInfoDTO;
import com.gomech.model.Organization;
import com.gomech.model.RefreshToken;
import com.gomech.model.User;
import com.gomech.service.MfaService;
import com.gomech.service.RefreshTokenService;
import org.springframework.transaction.annotation.Transactional;
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
import com.gomech.repository.OrganizationRepository;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/auth")
public class AuthController {
    @Autowired
    private AuthenticationManager authenticationManager;
    @Autowired
    private UserRepository repository;
    @Autowired
    private OrganizationRepository organizationRepository;
    @Autowired
    private TokenService tokenService;
    @Autowired
    private RefreshTokenService refreshTokenService;
    @Autowired
    private MfaService mfaService;

    @PostMapping("/login")
    @Transactional
    public ResponseEntity<LoginResponseDTO> login(@RequestBody @Valid AuthenticationDTO data) {
        var usernamePassword = new UsernamePasswordAuthenticationToken(data.email(), data.password());
        var auth = this.authenticationManager.authenticate(usernamePassword);

        var user = (User) auth.getPrincipal();
        if (user.isMfaEnabled()) {
            if (data.mfaCode() == null || !mfaService.verifyCode(user.getMfaSecret(), data.mfaCode())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new LoginResponseDTO(
                                null,
                                null,
                                true,
                                user.getEmail(),
                                user.getName(),
                                user.getRole().name(),
                                user.getId(),
                                OrganizationBasicInfoDTO.fromEntity(user.getOrganization())));
            }
        }

        var accessToken = tokenService.generateAccessToken(user);
        var refreshToken = refreshTokenService.createToken(user);

        return ResponseEntity.ok(new LoginResponseDTO(
                accessToken,
                refreshToken,
                false,
                user.getEmail(),
                user.getName(),
                user.getRole().name(),
                user.getId(),
                OrganizationBasicInfoDTO.fromEntity(user.getOrganization())
        ));
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterResponseDTO> register(@RequestBody @Valid RegisterDTO data){
        if (this.repository.findByEmail(data.email()).isPresent()) {
            return ResponseEntity.badRequest().build();
        }

        if (data.organizationId() == null) {
            return ResponseEntity.badRequest().build();
        }

        Organization organization = organizationRepository.findById(data.organizationId())
                .orElse(null);

        if (organization == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        String encryptedPassword = new BCryptPasswordEncoder().encode(data.password());
        User newUser = new User(data.name(), data.email(), encryptedPassword, data.role(), organization);

        String mfaSecret = null;
        if (data.mfaEnabled()) {
            String secret = mfaService.generateSecret();
            newUser.enableMfa(mfaService.encryptSecret(secret));
            mfaSecret = secret;
        }

        this.repository.save(newUser);

        return ResponseEntity.ok(new RegisterResponseDTO(newUser.getId(), data.mfaEnabled(), mfaSecret));
    }

    @PostMapping("/register-organization")
    @Transactional
    public ResponseEntity<?> registerOrganization(@RequestBody @Valid RegisterOrganizationDTO data) {
        // Check if admin email already exists
        if (this.repository.findByEmail(data.adminEmail()).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("Email already exists");
        }

        // Check if organization name already exists
        if (this.organizationRepository.existsByName(data.organizationName())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("Organization name already exists");
        }

        // Generate slug if not provided
        String slug = data.organizationSlug();
        if (slug == null || slug.isEmpty()) {
            slug = generateSlug(data.organizationName());
        }

        // Check if slug already exists
        if (this.organizationRepository.existsBySlug(slug)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("Organization slug already exists. Please choose a different name or slug.");
        }

        // Create organization
        Organization organization = new Organization(
                data.organizationName(),
                slug,
                data.organizationDescription(),
                data.organizationEmail(),
                data.organizationPhone(),
                data.organizationAddress(),
                data.organizationDocument()
        );
        Organization savedOrganization = organizationRepository.save(organization);

        // Create admin user
        String encryptedPassword = new BCryptPasswordEncoder().encode(data.adminPassword());
        User adminUser = new User(
                data.adminName(),
                data.adminEmail(),
                encryptedPassword,
                com.gomech.model.Role.ADMIN,
                savedOrganization
        );

        String mfaSecret = null;
        if (data.mfaEnabled()) {
            String secret = mfaService.generateSecret();
            adminUser.enableMfa(mfaService.encryptSecret(secret));
            mfaSecret = secret;
        }

        User savedUser = repository.save(adminUser);

        return ResponseEntity.status(HttpStatus.CREATED).body(new RegisterOrganizationResponseDTO(
                savedUser.getId(),
                savedUser.getEmail(),
                savedUser.getName(),
                OrganizationBasicInfoDTO.fromEntity(savedOrganization),
                data.mfaEnabled(),
                mfaSecret,
                "Organization and admin user created successfully"
        ));
    }

    private String generateSlug(String name) {
        return name.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
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
