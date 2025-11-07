package com.gomech.repository;

import com.gomech.model.TutorialProgress;
import com.gomech.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TutorialProgressRepository extends JpaRepository<TutorialProgress, Long> {
    
    Optional<TutorialProgress> findByUser(User user);
    
    Optional<TutorialProgress> findByUserId(Long userId);
    
    boolean existsByUserId(Long userId);
}

