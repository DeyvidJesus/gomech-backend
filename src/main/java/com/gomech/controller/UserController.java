package com.gomech.controller;

import com.gomech.dto.User.CreateUserDTO;
import com.gomech.dto.User.UserResponseDTO;
import com.gomech.model.Organization;
import com.gomech.model.Role;
import com.gomech.model.User;
import com.gomech.repository.UserRepository;
import com.gomech.service.MfaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "User management endpoints")
@SecurityRequirement(name = "bearer-key")
public class UserController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final MfaService mfaService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List users", description = "Get a paginated list of users in the same organization")
    public ResponseEntity<Page<UserResponseDTO>> findAll(
            Authentication authentication,
            @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        
        User currentUser = (User) authentication.getPrincipal();
        Organization organization = currentUser.getOrganization();
        
        Page<UserResponseDTO> users = userRepository
                .findByOrganization(organization, pageable)
                .map(UserResponseDTO::fromEntity);
        
        return ResponseEntity.ok(users);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get user by ID", description = "Get a single user by ID (same organization only)")
    public ResponseEntity<UserResponseDTO> findById(
            @PathVariable Long id,
            Authentication authentication) {
        
        User currentUser = (User) authentication.getPrincipal();
        
        return userRepository.findById(id)
                .filter(user -> user.getOrganization().getId().equals(currentUser.getOrganization().getId()))
                .map(UserResponseDTO::fromEntity)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create user", description = "Create a new user in the same organization (ADMIN only)")
    public ResponseEntity<?> create(
            @Valid @RequestBody CreateUserDTO dto,
            Authentication authentication) {
        
        User currentUser = (User) authentication.getPrincipal();
        Organization organization = currentUser.getOrganization();
        
        // Check if email already exists
        if (userRepository.findByEmail(dto.email()).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("Email already exists");
        }
        
        // Create new user
        String encryptedPassword = passwordEncoder.encode(dto.password());
        User newUser = new User(dto.name(), dto.email(), encryptedPassword, dto.role(), organization);
        
        // Handle MFA if enabled
        String mfaSecret = null;
        if (dto.mfaEnabled()) {
            String secret = mfaService.generateSecret();
            newUser.enableMfa(mfaService.encryptSecret(secret));
            mfaSecret = secret;
        }
        
        User savedUser = userRepository.save(newUser);
        
        // Return response with MFA secret if applicable
        if (mfaSecret != null) {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new UserCreationWithMfaResponse(
                            UserResponseDTO.fromEntity(savedUser),
                            mfaSecret
                    ));
        }
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(UserResponseDTO.fromEntity(savedUser));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete user", description = "Delete a user (ADMIN only, same organization)")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            Authentication authentication) {
        
        User currentUser = (User) authentication.getPrincipal();
        
        return userRepository.findById(id)
                .filter(user -> user.getOrganization().getId().equals(currentUser.getOrganization().getId()))
                .filter(user -> !user.getId().equals(currentUser.getId())) // Can't delete yourself
                .map(user -> {
                    userRepository.delete(user);
                    return ResponseEntity.noContent().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // Inner class for response with MFA
    public record UserCreationWithMfaResponse(
            UserResponseDTO user,
            String mfaSecret
    ) {}
}

