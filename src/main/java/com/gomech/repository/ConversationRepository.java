package com.gomech.repository;

import com.gomech.model.Conversation;
import com.gomech.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    Optional<Conversation> findByUserAndThreadId(User user, String threadId);

    @Query("SELECT c.threadId FROM Conversation c WHERE c.user.id = :userId")
    Optional<String> findThreadIdByUserId(Long userId);
}
