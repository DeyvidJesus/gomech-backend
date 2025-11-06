package com.gomech.controller;

import com.gomech.dto.Organization.OrganizationRequestDTO;
import com.gomech.dto.Organization.OrganizationResponseDTO;
import com.gomech.service.OrganizationService;
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
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/organizations")
@RequiredArgsConstructor
@Tag(name = "Organizations", description = "Organization management endpoints")
@SecurityRequirement(name = "bearer-key")
public class OrganizationController {

    private final OrganizationService organizationService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List all organizations", description = "Get a paginated list of all organizations")
    public ResponseEntity<Page<OrganizationResponseDTO>> findAll(
            @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(organizationService.findAll(pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get organization by ID", description = "Get a single organization by its ID")
    public ResponseEntity<OrganizationResponseDTO> findById(@PathVariable Long id) {
        return organizationService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/slug/{slug}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get organization by slug", description = "Get a single organization by its slug")
    public ResponseEntity<OrganizationResponseDTO> findBySlug(@PathVariable String slug) {
        return organizationService.findBySlug(slug)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create organization", description = "Create a new organization")
    public ResponseEntity<OrganizationResponseDTO> create(@Valid @RequestBody OrganizationRequestDTO dto) {
        OrganizationResponseDTO created = organizationService.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update organization", description = "Update an existing organization")
    public ResponseEntity<OrganizationResponseDTO> update(
            @PathVariable Long id,
            @Valid @RequestBody OrganizationRequestDTO dto) {
        try {
            OrganizationResponseDTO updated = organizationService.update(id, dto);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete organization", description = "Delete an organization by ID")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        try {
            organizationService.delete(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PatchMapping("/{id}/toggle-active")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Toggle organization active status", description = "Activate or deactivate an organization")
    public ResponseEntity<Void> toggleActive(@PathVariable Long id) {
        try {
            organizationService.toggleActive(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}

