package com.gomech.repository;

import com.gomech.model.Organization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrganizationRepository extends JpaRepository<Organization, Long> {
    
    Optional<Organization> findBySlug(String slug);
    
    Optional<Organization> findByName(String name);
    
    boolean existsBySlug(String slug);
    
    boolean existsByName(String name);
}

