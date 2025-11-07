package com.gomech.repository;

import com.gomech.model.Vehicle;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, Long> {
    Optional<Vehicle> findByLicensePlate(String licensePlate);
    
    // Organization-scoped queries
    Page<Vehicle> findByOrganizationId(Long organizationId, Pageable pageable);
    
    List<Vehicle> findByOrganizationId(Long organizationId);
    
    Optional<Vehicle> findByIdAndOrganizationId(Long id, Long organizationId);
    
    Optional<Vehicle> findByLicensePlateAndOrganizationId(String licensePlate, Long organizationId);
    
    List<Vehicle> findByClientIdAndOrganizationId(Long clientId, Long organizationId);
    
    long countByOrganizationId(Long organizationId);
}
