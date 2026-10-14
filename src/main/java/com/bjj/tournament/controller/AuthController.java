package com.bjj.tournament.controller;

import com.bjj.tournament.dto.AuthResponseDTO;
import com.bjj.tournament.dto.LoginRequestDTO;
import com.bjj.tournament.dto.UserRegistrationDTO;
import com.bjj.tournament.entity.User;
import com.bjj.tournament.security.JwtTokenProvider;
import com.bjj.tournament.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for authentication endpoints
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final JwtTokenProvider tokenProvider;

    /**
     * Register a new user
     * POST /api/auth/register
     */
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody UserRegistrationDTO registrationDTO) {
        log.info("Registration request received for username: {}", registrationDTO.getUsername());

        try {
            User user = userService.registerUser(registrationDTO);

            // Generate JWT token for the newly registered user
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            registrationDTO.getUsername(),
                            registrationDTO.getPassword()
                    )
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            String jwt = tokenProvider.generateToken(authentication);

            AuthResponseDTO response = new AuthResponseDTO(
                    jwt,
                    user.getId(),
                    user.getUsername(),
                    user.getEmail(),
                    user.getFullName(),
                    user.getRole()
            );
            response.setOrganizationId(user.getOrganizationId());

            log.info("User registered successfully: {}", user.getUsername());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalArgumentException e) {
            log.error("Registration failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Registration failed with unexpected error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Registration failed. Please try again."));
        }
    }

    /**
     * Login user
     * POST /api/auth/login
     */
    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@Valid @RequestBody LoginRequestDTO loginRequest) {
        log.info("Login request received for username: {}", loginRequest.getUsername());

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getUsername(),
                            loginRequest.getPassword()
                    )
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            String jwt = tokenProvider.generateToken(authentication);

            User user = userService.getUserByUsername(loginRequest.getUsername());

            AuthResponseDTO response = new AuthResponseDTO(
                    jwt,
                    user.getId(),
                    user.getUsername(),
                    user.getEmail(),
                    user.getFullName(),
                    user.getRole()
            );
            response.setOrganizationId(user.getOrganizationId());

            log.info("User logged in successfully: {}", user.getUsername());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Login failed for username: {}", loginRequest.getUsername(), e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Invalid username or password"));
        }
    }

    /**
     * Get current user information
     * GET /api/auth/me
     */
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Not authenticated"));
        }

        try {
            User user = userService.getUserByUsername(authentication.getName());

            AuthResponseDTO response = new AuthResponseDTO(
                    null, // No new token needed
                    user.getId(),
                    user.getUsername(),
                    user.getEmail(),
                    user.getFullName(),
                    user.getRole()
            );
            response.setOrganizationId(user.getOrganizationId());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to get current user", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Failed to get user information"));
        }
    }

    /**
     * Refresh JWT token
     * POST /api/auth/refresh
     * Requires a valid (not expired) token
     */
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Not authenticated"));
        }

        try {
            // Generate new token with fresh 24h expiration
            String newJwt = tokenProvider.generateToken(authentication);

            User user = userService.getUserByUsername(authentication.getName());

            AuthResponseDTO response = new AuthResponseDTO(
                    newJwt,
                    user.getId(),
                    user.getUsername(),
                    user.getEmail(),
                    user.getFullName(),
                    user.getRole()
            );
            response.setOrganizationId(user.getOrganizationId());

            log.info("Token refreshed successfully for user: {}", user.getUsername());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Token refresh failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Failed to refresh token"));
        }
    }

    /**
     * Logout user (client-side only - JWT is stateless)
     * POST /api/auth/logout
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logoutUser() {
        // JWT is stateless, so logout is handled on the client side
        // Client should remove the token from storage
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok(new MessageResponse("Logged out successfully"));
    }

    /**
     * TEMPORARY: List all users (for debugging)
     * GET /api/auth/users/all
     * Scoped to the caller's own organization - a platform admin sees everyone.
     */
    @GetMapping("/users/all")
    public ResponseEntity<?> getAllUsers() {
        log.info("Fetching all users from database");
        var users = userService.getAllUsers();

        // Return simplified user info (no passwords)
        var userInfo = users.stream()
            .map(u -> new UserInfo(u.getId(), u.getUsername(), u.getEmail(), u.getFullName(), u.getRole(), u.isEnabled(), u.getOrganizationId()))
            .toList();

        log.info("Found {} users", userInfo.size());
        return ResponseEntity.ok(userInfo);
    }

    /**
     * Change the current user's own password (self-service)
     * POST /api/auth/change-password
     */
    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@Valid @RequestBody ChangePasswordRequest request,
                                             Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Not authenticated"));
        }

        try {
            User user = userService.getUserByUsername(authentication.getName());
            userService.changePassword(user, request.currentPassword(), request.newPassword());
            return ResponseEntity.ok(new MessageResponse("Password changed successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to change password", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Failed to change password"));
        }
    }

    /**
     * Admin-initiated password reset for another user (e.g. a forgotten
     * password, with no self-service email flow yet). Open to ROLE_ADMIN
     * and ROLE_PLATFORM_ADMIN only (enforced in SecurityConfig); the actual
     * cross-org guard happens inside UserService.getUserById.
     * PUT /api/auth/users/{id}/reset-password
     */
    @PutMapping("/users/{id}/reset-password")
    public ResponseEntity<?> adminResetPassword(@PathVariable Long id,
                                                 @Valid @RequestBody AdminResetPasswordRequest request) {
        userService.adminResetPassword(id, request.newPassword());
        return ResponseEntity.ok(new MessageResponse("Password reset successfully"));
    }

    /**
     * User info response (no password)
     */
    record UserInfo(Long id, String username, String email, String fullName, String role, Boolean enabled, Long organizationId) {}

    /**
     * Change password request
     */
    record ChangePasswordRequest(
        @jakarta.validation.constraints.NotBlank(message = "Current password is required") String currentPassword,
        @jakarta.validation.constraints.NotBlank(message = "New password is required")
        @jakarta.validation.constraints.Size(min = 6, max = 100, message = "New password must be between 6 and 100 characters")
        String newPassword
    ) {}

    /**
     * Admin password-reset request (no current password needed)
     */
    record AdminResetPasswordRequest(
        @jakarta.validation.constraints.NotBlank(message = "New password is required")
        @jakarta.validation.constraints.Size(min = 6, max = 100, message = "New password must be between 6 and 100 characters")
        String newPassword
    ) {}

    /**
     * Error response class
     */
    record ErrorResponse(String message) {}

    /**
     * Message response class
     */
    record MessageResponse(String message) {}
}
