package com.gomech.dto.Authentication;

import com.gomech.dto.Organization.OrganizationBasicInfoDTO;

public record RegisterOrganizationResponseDTO(
    Long userId,
    String userEmail,
    String userName,
    OrganizationBasicInfoDTO organization,
    boolean mfaEnabled,
    String mfaSecret,
    String message
) {}

