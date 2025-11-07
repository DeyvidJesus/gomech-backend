package com.gomech.dto.User;

import com.gomech.dto.Organization.OrganizationBasicInfoDTO;
import com.gomech.model.Role;
import com.gomech.model.User;

public record UserResponseDTO(
    Long id,
    String name,
    String email,
    Role role,
    boolean mfaEnabled,
    OrganizationBasicInfoDTO organization
) {
    public static UserResponseDTO fromEntity(User user) {
        return new UserResponseDTO(
            user.getId(),
            user.getName(),
            user.getEmail(),
            user.getRole(),
            user.isMfaEnabled(),
            OrganizationBasicInfoDTO.fromEntity(user.getOrganization())
        );
    }
}

