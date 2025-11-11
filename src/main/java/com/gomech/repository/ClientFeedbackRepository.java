package com.gomech.repository;

import com.gomech.model.ClientFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ClientFeedbackRepository extends JpaRepository<ClientFeedback, Long> {
    
    List<ClientFeedback> findByClientIdOrderByCreatedAtDesc(Long clientId);
    
    List<ClientFeedback> findByServiceOrderIdOrderByCreatedAtDesc(Long serviceOrderId);
    
    @Query("SELECT f FROM ClientFeedback f WHERE f.resolved = :resolved " +
           "ORDER BY f.createdAt DESC")
    List<ClientFeedback> findByResolved(@Param("resolved") Boolean resolved);
    
    @Query("SELECT AVG(f.npsScore) FROM ClientFeedback f WHERE f.npsScore IS NOT NULL " +
           "AND f.createdAt >= :startDate")
    Double calculateAverageNPS(@Param("startDate") LocalDateTime startDate);
    
    @Query("SELECT AVG(f.rating) FROM ClientFeedback f WHERE f.rating IS NOT NULL " +
           "AND f.createdAt >= :startDate")
    Double calculateAverageRating(@Param("startDate") LocalDateTime startDate);
}

