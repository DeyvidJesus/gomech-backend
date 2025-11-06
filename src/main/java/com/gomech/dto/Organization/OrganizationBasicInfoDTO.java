package com.gomech.dto.Organization;

import com.gomech.model.Organization;

public record OrganizationBasicInfoDTO(
        Long id,
        String name,
        String slug,
        Boolean active,
        String contactEmail,
        String contactPhone,
        String address,
        String document
) {

    public static OrganizationBasicInfoDTO fromEntity(Organization organization) {
        if (organization == null) {
            return null;
        }

        return new OrganizationBasicInfoDTO(
                organization.getId(),
                organization.getName(),
                organization.getSlug(),
                organization.getActive(),
                organization.getContactEmail(),
                organization.getContactPhone(),
                organization.getAddress(),
                organization.getDocument()
        );
    }
}
