package com.gomech.dto.Authentication;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterOrganizationDTO(
    // Organization data
    @NotBlank(message = "Organization name is required")
    @Size(max = 100, message = "Organization name must not exceed 100 characters")
    String organizationName,
    
    @Size(max = 50, message = "Slug must not exceed 50 characters")
    String organizationSlug,
    
    String organizationDescription,
    
    @Email(message = "Invalid organization email format")
    String organizationEmail,
    
    @Size(max = 20, message = "Phone must not exceed 20 characters")
    String organizationPhone,
    
    String organizationAddress,
    
    @Size(max = 50, message = "Document must not exceed 50 characters")
    String organizationDocument,
    
    // Admin user data
    @NotBlank(message = "Admin name is required")
    String adminName,
    
    @NotBlank(message = "Admin email is required")
    @Email(message = "Invalid admin email format")
    String adminEmail,
    
    @NotBlank(message = "Admin password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    String adminPassword,
    
    boolean mfaEnabled
) {}

