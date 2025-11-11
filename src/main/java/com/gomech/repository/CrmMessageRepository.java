package com.gomech.repository;

import com.gomech.model.CrmMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface CrmMessageRepository extends JpaRepository<CrmMessage, Long> {
    
    List<CrmMessage> findByClientIdOrderByCreatedAtDesc(Long clientId);
    
    List<CrmMessage> findByPhoneNumberOrderByCreatedAtDesc(String phoneNumber);
    
    @Query("SELECT m FROM CrmMessage m WHERE m.client.id = :clientId " +
           "AND m.createdAt >= :startDate ORDER BY m.createdAt DESC")
    List<CrmMessage> findByClientIdAndDateRange(@Param("clientId") Long clientId, 
                                                 @Param("startDate") LocalDateTime startDate);
    
    @Query("SELECT m FROM CrmMessage m WHERE m.sentiment = :sentiment " +
           "ORDER BY m.createdAt DESC")
    List<CrmMessage> findBySentiment(@Param("sentiment") CrmMessage.Sentiment sentiment);
    
    @Query("SELECT m FROM CrmMessage m WHERE m.status = :status " +
           "ORDER BY m.createdAt DESC")
    List<CrmMessage> findByStatus(@Param("status") CrmMessage.MessageStatus status);
}

