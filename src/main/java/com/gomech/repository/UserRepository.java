package com.gomech.repository;

import com.gomech.model.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    @EntityGraph(attributePaths = "organization")
    Optional<User> findWithOrganizationByEmail(String email);
}
