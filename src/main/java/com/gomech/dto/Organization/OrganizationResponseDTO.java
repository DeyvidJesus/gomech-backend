package com.gomech.dto.Organization;

import com.gomech.model.Organization;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationResponseDTO {
    
    private Long id;
    private String name;
    private String slug;
    private String description;
    private Boolean active;
    private String contactEmail;
    private String contactPhone;
    private String address;
    private String document;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    public static OrganizationResponseDTO fromEntity(Organization organization) {
        return new OrganizationResponseDTO(
                organization.getId(),
                organization.getName(),
                organization.getSlug(),
                organization.getDescription(),
                organization.getActive(),
                organization.getContactEmail(),
                organization.getContactPhone(),
                organization.getAddress(),
                organization.getDocument(),
                organization.getCreatedAt(),
                organization.getUpdatedAt()
        );
    }
}

