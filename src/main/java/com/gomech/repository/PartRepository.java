package com.gomech.repository;

import com.gomech.domain.Part;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PartRepository extends JpaRepository<Part, Long> {
    Optional<Part> findBySku(String sku);
    
    // Organization-scoped queries
    Page<Part> findByOrganizationId(Long organizationId, Pageable pageable);
    
    List<Part> findByOrganizationId(Long organizationId);
    
    Optional<Part> findByIdAndOrganizationId(Long id, Long organizationId);
    
    Optional<Part> findBySkuAndOrganizationId(String sku, Long organizationId);
    
    List<Part> findByActiveAndOrganizationId(Boolean active, Long organizationId);
    
    long countByOrganizationId(Long organizationId);
}
