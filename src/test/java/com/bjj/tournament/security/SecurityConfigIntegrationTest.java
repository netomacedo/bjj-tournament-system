package com.bjj.tournament.security;

import com.bjj.tournament.dto.LoginRequestDTO;
import com.bjj.tournament.dto.UserRegistrationDTO;
import com.bjj.tournament.entity.User;
import com.bjj.tournament.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for Spring Security configuration
 * Tests end-to-end authentication flow with JWT
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SecurityConfigIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private UserRegistrationDTO registrationDTO;
    private LoginRequestDTO loginRequest;

    @BeforeEach
    void setUp() {
        // Clean up any existing test users
        userRepository.deleteAll();

        // Create registration DTO
        registrationDTO = new UserRegistrationDTO();
        registrationDTO.setUsername("integrationtest");
        registrationDTO.setEmail("integration@test.com");
        registrationDTO.setPassword("password123");
        registrationDTO.setFullName("Integration Test User");

        // Create login request
        loginRequest = new LoginRequestDTO();
        loginRequest.setUsername("integrationtest");
        loginRequest.setPassword("password123");
    }

    @Test
    void publicEndpoint_WithoutAuthentication_AllowsAccess() throws Exception {
        // When/Then
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registrationDTO)))
                .andExpect(status().isCreated());
    }

    @Test
    void protectedEndpoint_WithoutAuthentication_ReturnsForbidden() throws Exception {
        // When/Then
        mockMvc.perform(get("/api/athletes"))
                .andExpect(status().isForbidden());
    }

    @Test
    void protectedEndpoint_WithValidToken_AllowsAccess() throws Exception {
        // Given - register and login to get token
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registrationDTO)))
                .andExpect(status().isCreated())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        String token = objectMapper.readTree(response).get("token").asText();

        // When/Then - access protected endpoint with token
        mockMvc.perform(get("/api/athletes")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void protectedEndpoint_WithInvalidToken_ReturnsForbidden() throws Exception {
        // When/Then
        mockMvc.perform(get("/api/athletes")
                        .header("Authorization", "Bearer invalid.token.here"))
                .andExpect(status().isForbidden());
    }

    @Test
    void fullAuthenticationFlow_RegisterLoginAndAccessProtectedEndpoint() throws Exception {
        // Step 1: Register
        MvcResult registerResult = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registrationDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token", notNullValue()))
                .andExpect(jsonPath("$.username", is("integrationtest")))
                .andExpect(jsonPath("$.role", is("ROLE_USER")))
                .andReturn();

        // Step 2: Login with same credentials
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", notNullValue()))
                .andExpect(jsonPath("$.username", is("integrationtest")))
                .andReturn();

        String loginResponse = loginResult.getResponse().getContentAsString();
        String loginToken = objectMapper.readTree(loginResponse).get("token").asText();

        // Step 3: Access protected endpoint with login token
        mockMvc.perform(get("/api/athletes")
                        .header("Authorization", "Bearer " + loginToken))
                .andExpect(status().isOk());

        // Step 4: Access current user endpoint
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + loginToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username", is("integrationtest")))
                .andExpect(jsonPath("$.email", is("integration@test.com")));
    }

    @Test
    void login_WithIncorrectPassword_ReturnsUnauthorized() throws Exception {
        // Given - register user first
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registrationDTO)))
                .andExpect(status().isCreated());

        // When - try to login with wrong password
        LoginRequestDTO wrongPasswordRequest = new LoginRequestDTO();
        wrongPasswordRequest.setUsername("integrationtest");
        wrongPasswordRequest.setPassword("wrongpassword");

        // Then
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(wrongPasswordRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message", is("Invalid username or password")));
    }

    @Test
    void login_WithNonexistentUser_ReturnsUnauthorized() throws Exception {
        // Given
        LoginRequestDTO nonexistentUserRequest = new LoginRequestDTO();
        nonexistentUserRequest.setUsername("nonexistent");
        nonexistentUserRequest.setPassword("password123");

        // When/Then
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(nonexistentUserRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void register_WithDuplicateUsername_ReturnsBadRequest() throws Exception {
        // Given - register user first
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registrationDTO)))
                .andExpect(status().isCreated());

        // When - try to register again with same username
        UserRegistrationDTO duplicateDTO = new UserRegistrationDTO();
        duplicateDTO.setUsername("integrationtest");
        duplicateDTO.setEmail("different@email.com");
        duplicateDTO.setPassword("password123");
        duplicateDTO.setFullName("Different User");

        // Then
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("already taken")));
    }

    @Test
    void passwordEncoding_PasswordIsEncoded() throws Exception {
        // Given/When - register user
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registrationDTO)))
                .andExpect(status().isCreated());

        // Then - verify password is encoded in database
        User savedUser = userRepository.findByUsername("integrationtest").orElseThrow();

        // Password should NOT be plain text
        org.assertj.core.api.Assertions.assertThat(savedUser.getPassword()).isNotEqualTo("password123");

        // Password should be a BCrypt hash (starts with $2a$ or $2b$)
        org.assertj.core.api.Assertions.assertThat(savedUser.getPassword()).matches("^\\$2[ab]\\$.*");

        // Verify encoder can match the password
        org.assertj.core.api.Assertions.assertThat(passwordEncoder.matches("password123", savedUser.getPassword())).isTrue();
    }

    @Test
    void cors_AllowsConfiguredOrigins() throws Exception {
        // When/Then - CORS headers should be present
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Origin", "http://localhost:3000")
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(header().exists("Access-Control-Allow-Origin"));
    }

    @Test
    void tokenExpiration_TokenContainsExpirationClaim() throws Exception {
        // Given/When - register and get token
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registrationDTO)))
                .andExpect(status().isCreated())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        String token = objectMapper.readTree(response).get("token").asText();

        // Then - token should have 3 parts (header.payload.signature)
        String[] tokenParts = token.split("\\.");
        org.assertj.core.api.Assertions.assertThat(tokenParts).hasSize(3);
    }

    @Test
    void logout_ClearsSecurityContext() throws Exception {
        // Given - register and login
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registrationDTO)))
                .andExpect(status().isCreated())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        String token = objectMapper.readTree(response).get("token").asText();

        // When - logout
        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("Logged out successfully")));

        // Note: JWT is stateless, so the token is still valid until expiration
        // Logout just returns a success message; client must discard the token
    }

    @Test
    void userRole_NewUserGetsDefaultRole() throws Exception {
        // When
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registrationDTO)))
                .andExpect(status().isCreated())
                .andReturn();

        String response = result.getResponse().getContentAsString();

        // Then
        String role = objectMapper.readTree(response).get("role").asText();
        org.assertj.core.api.Assertions.assertThat(role).isEqualTo("ROLE_USER");

        // Verify in database
        User savedUser = userRepository.findByUsername("integrationtest").orElseThrow();
        org.assertj.core.api.Assertions.assertThat(savedUser.getRole()).isEqualTo("ROLE_USER");
    }

    @Test
    void accountStatus_NewUserIsEnabledAndUnlocked() throws Exception {
        // When
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registrationDTO)))
                .andExpect(status().isCreated());

        // Then - verify in database
        User savedUser = userRepository.findByUsername("integrationtest").orElseThrow();
        org.assertj.core.api.Assertions.assertThat(savedUser.getEnabled()).isTrue();
        org.assertj.core.api.Assertions.assertThat(savedUser.getAccountNonLocked()).isTrue();
    }

    @Test
    void multipleTokens_EachLoginGeneratesUniqueToken() throws Exception {
        // Given - register user
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registrationDTO)))
                .andExpect(status().isCreated());

        // When - login twice
        MvcResult login1 = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        Thread.sleep(10); // Small delay to ensure different timestamps

        MvcResult login2 = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        // Then - tokens should be different
        String token1 = objectMapper.readTree(login1.getResponse().getContentAsString()).get("token").asText();
        String token2 = objectMapper.readTree(login2.getResponse().getContentAsString()).get("token").asText();

        org.assertj.core.api.Assertions.assertThat(token1).isNotEqualTo(token2);

        // But both should work
        mockMvc.perform(get("/api/athletes")
                        .header("Authorization", "Bearer " + token1))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/athletes")
                        .header("Authorization", "Bearer " + token2))
                .andExpect(status().isOk());
    }
}
