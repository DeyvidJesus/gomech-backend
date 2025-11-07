package com.gomech.dto.Authentication;

import com.gomech.dto.Organization.OrganizationBasicInfoDTO;

public record LoginResponseDTO(
        String accessToken,
        String refreshToken,
        boolean mfaRequired,
        String email,
        String name,
        String role,
        Long id,
        OrganizationBasicInfoDTO organization
) {
}
