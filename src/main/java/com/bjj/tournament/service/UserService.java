package com.bjj.tournament.service;

import com.bjj.tournament.dto.UserRegistrationDTO;
import com.bjj.tournament.entity.User;
import com.bjj.tournament.exception.OrganizationAccessDeniedException;
import com.bjj.tournament.repository.UserRepository;
import com.bjj.tournament.security.OrganizationAccessGuard;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

/**
 * Service for managing user operations
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private static final Set<String> VALID_ROLES = Set.of("ROLE_USER", "ROLE_ADMIN", "ROLE_PLATFORM_ADMIN");

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final OrganizationAccessGuard organizationAccessGuard;

    /**
     * Register a new user, inheriting the creating admin's organization.
     * The org is never client-supplied - an org-A admin has no way to
     * assign a new user to org B, since it's always derived from the
     * currently authenticated caller (via OrganizationAccessGuard), never
     * from anything in the request body.
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

        // A platform admin has no organization of their own to hand down -
        // creating an organization's first admin goes through
        // PlatformController instead, which supplies the org explicitly.
        Long organizationId = organizationAccessGuard.currentOrganizationId();
        if (organizationId == null) {
            throw new IllegalStateException(
                "A platform admin cannot create an ordinary user directly - use the platform organization endpoint");
        }

        // Create new user
        User user = new User();
        user.setOrganizationId(organizationId);
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
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + id));
        organizationAccessGuard.checkAccess(user.getOrganizationId());
        return user;
    }

    /**
     * Get all users - every user for a platform admin, or just the caller's
     * own organization's users otherwise.
     */
    @Transactional(readOnly = true)
    public List<User> getAllUsers() {
        if (organizationAccessGuard.isPlatformAdmin()) {
            return userRepository.findAll();
        }
        return userRepository.findByOrganizationId(organizationAccessGuard.currentOrganizationId());
    }

    /**
     * Update user role. Validates the role is one of the known values, and
     * enforces who can grant what: only a platform admin can grant the
     * platform role. Modifying a user outside the caller's own organization
     * is already ruled out by getUserById's ownership check below.
     */
    @Transactional
    public User updateUserRole(Long userId, String role) {
        log.info("Updating role for user ID: {} to {}", userId, role);

        if (!VALID_ROLES.contains(role)) {
            throw new IllegalArgumentException("Invalid role: " + role);
        }

        if ("ROLE_PLATFORM_ADMIN".equals(role) && !organizationAccessGuard.isPlatformAdmin()) {
            throw new OrganizationAccessDeniedException("Only a platform admin can grant the platform admin role");
        }

        User user = getUserById(userId);
        user.setRole(role);

        User updatedUser = userRepository.save(user);
        log.info("Successfully updated role for user ID: {}", userId);

        return updatedUser;
    }

    /**
     * Change a user's own password (self-service, e.g. to rotate the
     * one-time bootstrap password after first login)
     */
    @Transactional
    public void changePassword(User user, String currentPassword, String newPassword) {
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        log.info("Password changed for user ID: {}", user.getId());
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
