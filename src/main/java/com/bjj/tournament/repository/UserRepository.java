package com.bjj.tournament.repository;

import com.bjj.tournament.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for User entity
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Find user by username
     */
    Optional<User> findByUsername(String username);

    /**
     * Find user by email
     */
    Optional<User> findByEmail(String email);

    /**
     * Check if username already exists
     */
    boolean existsByUsername(String username);

    /**
     * Check if email already exists
     */
    boolean existsByEmail(String email);
}
