package com.gomech.repository;

import com.gomech.model.Client;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClientRepository extends JpaRepository<Client, Long> {
    Optional<Client> findByDocument(String document);
    
    // Organization-scoped queries
    Page<Client> findByOrganizationId(Long organizationId, Pageable pageable);
    
    List<Client> findByOrganizationId(Long organizationId);
    
    Optional<Client> findByIdAndOrganizationId(Long id, Long organizationId);
    
    Optional<Client> findByDocumentAndOrganizationId(String document, Long organizationId);
    
    long countByOrganizationId(Long organizationId);
}
