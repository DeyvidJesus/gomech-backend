package com.gomech.service;

import com.gomech.dto.Organization.OrganizationRequestDTO;
import com.gomech.dto.Organization.OrganizationResponseDTO;
import com.gomech.model.Organization;
import com.gomech.repository.OrganizationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrganizationService {

    private final OrganizationRepository organizationRepository;

    @Transactional(readOnly = true)
    public Page<OrganizationResponseDTO> findAll(Pageable pageable) {
        return organizationRepository.findAll(pageable)
                .map(OrganizationResponseDTO::fromEntity);
    }

    @Transactional(readOnly = true)
    public Optional<OrganizationResponseDTO> findById(Long id) {
        return organizationRepository.findById(id)
                .map(OrganizationResponseDTO::fromEntity);
    }

    @Transactional(readOnly = true)
    public Optional<OrganizationResponseDTO> findBySlug(String slug) {
        return organizationRepository.findBySlug(slug)
                .map(OrganizationResponseDTO::fromEntity);
    }

    @Transactional
    public OrganizationResponseDTO create(OrganizationRequestDTO dto) {
        // Validate unique name
        if (organizationRepository.existsByName(dto.getName())) {
            throw new IllegalArgumentException("Organization with name '" + dto.getName() + "' already exists");
        }

        // Validate unique slug if provided
        if (dto.getSlug() != null && !dto.getSlug().isEmpty()) {
            if (organizationRepository.existsBySlug(dto.getSlug())) {
                throw new IllegalArgumentException("Organization with slug '" + dto.getSlug() + "' already exists");
            }
        } else {
            // Generate slug from name if not provided
            dto.setSlug(generateSlug(dto.getName()));
        }

        Organization organization = new Organization(
                dto.getName(),
                dto.getSlug(),
                dto.getDescription(),
                dto.getContactEmail(),
                dto.getContactPhone(),
                dto.getAddress(),
                dto.getDocument()
        );

        Organization saved = organizationRepository.save(organization);
        log.info("Organization created: {} (ID: {})", saved.getName(), saved.getId());
        
        return OrganizationResponseDTO.fromEntity(saved);
    }

    @Transactional
    public OrganizationResponseDTO update(Long id, OrganizationRequestDTO dto) {
        Organization organization = organizationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Organization not found with id: " + id));

        // Validate unique name if changed
        if (!organization.getName().equals(dto.getName())) {
            if (organizationRepository.existsByName(dto.getName())) {
                throw new IllegalArgumentException("Organization with name '" + dto.getName() + "' already exists");
            }
        }

        // Validate unique slug if changed
        if (dto.getSlug() != null && !organization.getSlug().equals(dto.getSlug())) {
            if (organizationRepository.existsBySlug(dto.getSlug())) {
                throw new IllegalArgumentException("Organization with slug '" + dto.getSlug() + "' already exists");
            }
        }

        organization.setName(dto.getName());
        organization.setSlug(dto.getSlug() != null ? dto.getSlug() : generateSlug(dto.getName()));
        organization.setDescription(dto.getDescription());
        organization.setContactEmail(dto.getContactEmail());
        organization.setContactPhone(dto.getContactPhone());
        organization.setAddress(dto.getAddress());
        organization.setDocument(dto.getDocument());

        Organization updated = organizationRepository.save(organization);
        log.info("Organization updated: {} (ID: {})", updated.getName(), updated.getId());
        
        return OrganizationResponseDTO.fromEntity(updated);
    }

    @Transactional
    public void delete(Long id) {
        Organization organization = organizationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Organization not found with id: " + id));
        
        organizationRepository.delete(organization);
        log.info("Organization deleted: {} (ID: {})", organization.getName(), id);
    }

    @Transactional
    public void toggleActive(Long id) {
        Organization organization = organizationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Organization not found with id: " + id));
        
        organization.setActive(!organization.getActive());
        organizationRepository.save(organization);
        log.info("Organization active status toggled: {} (ID: {}) - Active: {}", 
                organization.getName(), id, organization.getActive());
    }

    private String generateSlug(String name) {
        return name.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .trim();
    }
}

