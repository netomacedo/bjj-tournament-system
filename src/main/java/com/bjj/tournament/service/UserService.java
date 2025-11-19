package com.bjj.tournament.service;

import com.bjj.tournament.dto.UserRegistrationDTO;
import com.bjj.tournament.entity.User;
import com.bjj.tournament.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service for managing user operations
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Register a new user
     */
    @Transactional
    public User registerUser(UserRegistrationDTO registrationDTO) {
        log.info("Registering new user: {}", registrationDTO.getUsername());

        // Check if username already exists
        if (userRepository.existsByUsername(registrationDTO.getUsername())) {
            throw new IllegalArgumentException("Username '" + registrationDTO.getUsername() + "' is already taken");
        }

        // Check if email already exists
        if (userRepository.existsByEmail(registrationDTO.getEmail())) {
            throw new IllegalArgumentException("Email '" + registrationDTO.getEmail() + "' is already registered");
        }

        // Create new user
        User user = new User();
        user.setUsername(registrationDTO.getUsername());
        user.setEmail(registrationDTO.getEmail());
        user.setPassword(passwordEncoder.encode(registrationDTO.getPassword()));
        user.setFullName(registrationDTO.getFullName());
        user.setRole("ROLE_USER");
        user.setEnabled(true);
        user.setAccountNonLocked(true);

        User savedUser = userRepository.save(user);
        log.info("Successfully registered user with ID: {}", savedUser.getId());

        return savedUser;
    }

    /**
     * Get user by username
     */
    @Transactional(readOnly = true)
    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found with username: " + username));
    }

    /**
     * Get user by ID
     */
    @Transactional(readOnly = true)
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + id));
    }

    /**
     * Get all users
     */
    @Transactional(readOnly = true)
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    /**
     * Update user role (admin only)
     */
    @Transactional
    public User updateUserRole(Long userId, String role) {
        log.info("Updating role for user ID: {} to {}", userId, role);

        User user = getUserById(userId);
        user.setRole(role);

        User updatedUser = userRepository.save(user);
        log.info("Successfully updated role for user ID: {}", userId);

        return updatedUser;
    }

    /**
     * Enable or disable user account (admin only)
     */
    @Transactional
    public User updateUserStatus(Long userId, boolean enabled) {
        log.info("Updating status for user ID: {} to {}", userId, enabled);

        User user = getUserById(userId);
        user.setEnabled(enabled);

        User updatedUser = userRepository.save(user);
        log.info("Successfully updated status for user ID: {}", userId);

        return updatedUser;
    }

    /**
     * Delete user
     */
    @Transactional
    public void deleteUser(Long id) {
        log.info("Deleting user with ID: {}", id);

        User user = getUserById(id);
        userRepository.delete(user);

        log.info("Successfully deleted user with ID: {}", id);
    }
}
