package com.bjj.tournament.controller;

import com.bjj.tournament.dto.AuthResponseDTO;
import com.bjj.tournament.dto.LoginRequestDTO;
import com.bjj.tournament.dto.UserRegistrationDTO;
import com.bjj.tournament.entity.User;
import com.bjj.tournament.security.JwtTokenProvider;
import com.bjj.tournament.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for AuthController
 * Tests authentication endpoints including registration, login, and user info
 */
@WebMvcTest(AuthController.class)
@ContextConfiguration
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthenticationManager authenticationManager;

    @MockBean
    private UserService userService;

    @MockBean
    private JwtTokenProvider tokenProvider;

    private User testUser;
    private UserRegistrationDTO registrationDTO;
    private LoginRequestDTO loginRequest;

    @BeforeEach
    void setUp() {
        // Create test user
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword("encodedPassword");
        testUser.setFullName("Test User");
        testUser.setRole("ROLE_USER");
        testUser.setEnabled(true);
        testUser.setAccountNonLocked(true);

        // Create registration DTO
        registrationDTO = new UserRegistrationDTO();
        registrationDTO.setUsername("testuser");
        registrationDTO.setEmail("test@example.com");
        registrationDTO.setPassword("password123");
        registrationDTO.setFullName("Test User");

        // Create login request
        loginRequest = new LoginRequestDTO();
        loginRequest.setUsername("testuser");
        loginRequest.setPassword("password123");
    }

    @Test
    void registerUser_WithValidData_ReturnsCreatedWithToken() throws Exception {
        // Given
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                testUser, null, testUser.getAuthorities()
        );
        String mockToken = "mock.jwt.token";

        when(userService.registerUser(any(UserRegistrationDTO.class))).thenReturn(testUser);
        when(authenticationManager.authenticate(any(Authentication.class))).thenReturn(authentication);
        when(tokenProvider.generateToken(any(Authentication.class))).thenReturn(mockToken);

        // When/Then
        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registrationDTO)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.token", is(mockToken)))
                .andExpect(jsonPath("$.type", is("Bearer")))
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.username", is("testuser")))
                .andExpect(jsonPath("$.email", is("test@example.com")))
                .andExpect(jsonPath("$.fullName", is("Test User")))
                .andExpect(jsonPath("$.role", is("ROLE_USER")));

        verify(userService).registerUser(any(UserRegistrationDTO.class));
        verify(authenticationManager).authenticate(any(Authentication.class));
        verify(tokenProvider).generateToken(any(Authentication.class));
    }

    @Test
    void registerUser_WithDuplicateUsername_ReturnsBadRequest() throws Exception {
        // Given
        when(userService.registerUser(any(UserRegistrationDTO.class)))
                .thenThrow(new IllegalArgumentException("Username 'testuser' is already taken"));

        // When/Then
        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registrationDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Username 'testuser' is already taken")));

        verify(userService).registerUser(any(UserRegistrationDTO.class));
        verify(authenticationManager, never()).authenticate(any());
    }

    @Test
    void registerUser_WithDuplicateEmail_ReturnsBadRequest() throws Exception {
        // Given
        when(userService.registerUser(any(UserRegistrationDTO.class)))
                .thenThrow(new IllegalArgumentException("Email 'test@example.com' is already registered"));

        // When/Then
        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registrationDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Email 'test@example.com' is already registered")));
    }

    @Test
    void registerUser_WithInvalidData_ReturnsBadRequest() throws Exception {
        // Given - invalid registration DTO (empty username)
        UserRegistrationDTO invalidDTO = new UserRegistrationDTO();
        invalidDTO.setUsername("");
        invalidDTO.setEmail("test@example.com");
        invalidDTO.setPassword("password123");
        invalidDTO.setFullName("Test User");

        // When/Then
        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDTO)))
                .andExpect(status().isBadRequest());

        verify(userService, never()).registerUser(any());
    }

    @Test
    void loginUser_WithValidCredentials_ReturnsOkWithToken() throws Exception {
        // Given
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                testUser, null, testUser.getAuthorities()
        );
        String mockToken = "mock.jwt.token";

        when(authenticationManager.authenticate(any(Authentication.class))).thenReturn(authentication);
        when(tokenProvider.generateToken(any(Authentication.class))).thenReturn(mockToken);
        when(userService.getUserByUsername(anyString())).thenReturn(testUser);

        // When/Then
        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.token", is(mockToken)))
                .andExpect(jsonPath("$.type", is("Bearer")))
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.username", is("testuser")))
                .andExpect(jsonPath("$.email", is("test@example.com")))
                .andExpect(jsonPath("$.fullName", is("Test User")))
                .andExpect(jsonPath("$.role", is("ROLE_USER")));

        verify(authenticationManager).authenticate(any(Authentication.class));
        verify(tokenProvider).generateToken(any(Authentication.class));
        verify(userService).getUserByUsername("testuser");
    }

    @Test
    void loginUser_WithInvalidCredentials_ReturnsUnauthorized() throws Exception {
        // Given
        when(authenticationManager.authenticate(any(Authentication.class)))
                .thenThrow(new BadCredentialsException("Invalid username or password"));

        // When/Then
        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message", is("Invalid username or password")));

        verify(authenticationManager).authenticate(any(Authentication.class));
        verify(tokenProvider, never()).generateToken(any());
        verify(userService, never()).getUserByUsername(anyString());
    }

    @Test
    void loginUser_WithInvalidData_ReturnsBadRequest() throws Exception {
        // Given - invalid login request (empty username)
        LoginRequestDTO invalidRequest = new LoginRequestDTO();
        invalidRequest.setUsername("");
        invalidRequest.setPassword("password123");

        // When/Then
        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(authenticationManager, never()).authenticate(any());
    }

    @Test
    @WithMockUser(username = "testuser")
    void getCurrentUser_WhenAuthenticated_ReturnsUserInfo() throws Exception {
        // Given
        when(userService.getUserByUsername("testuser")).thenReturn(testUser);

        // When/Then
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.username", is("testuser")))
                .andExpect(jsonPath("$.email", is("test@example.com")))
                .andExpect(jsonPath("$.fullName", is("Test User")))
                .andExpect(jsonPath("$.role", is("ROLE_USER")));

        verify(userService).getUserByUsername("testuser");
    }

    @Test
    void getCurrentUser_WhenNotAuthenticated_ReturnsUnauthorized() throws Exception {
        // When/Then
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());

        verify(userService, never()).getUserByUsername(anyString());
    }

    @Test
    @WithMockUser(username = "testuser")
    void logoutUser_ReturnsSuccess() throws Exception {
        // When/Then
        mockMvc.perform(post("/api/auth/logout")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("Logged out successfully")));
    }

    @Test
    void registerUser_WithShortPassword_ReturnsBadRequest() throws Exception {
        // Given - password too short
        UserRegistrationDTO shortPasswordDTO = new UserRegistrationDTO();
        shortPasswordDTO.setUsername("testuser");
        shortPasswordDTO.setEmail("test@example.com");
        shortPasswordDTO.setPassword("123");
        shortPasswordDTO.setFullName("Test User");

        // When/Then
        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(shortPasswordDTO)))
                .andExpect(status().isBadRequest());

        verify(userService, never()).registerUser(any());
    }

    @Test
    void registerUser_WithInvalidEmail_ReturnsBadRequest() throws Exception {
        // Given - invalid email format
        UserRegistrationDTO invalidEmailDTO = new UserRegistrationDTO();
        invalidEmailDTO.setUsername("testuser");
        invalidEmailDTO.setEmail("invalid-email");
        invalidEmailDTO.setPassword("password123");
        invalidEmailDTO.setFullName("Test User");

        // When/Then
        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidEmailDTO)))
                .andExpect(status().isBadRequest());

        verify(userService, never()).registerUser(any());
    }

    @Test
    void registerUser_WithShortUsername_ReturnsBadRequest() throws Exception {
        // Given - username too short
        UserRegistrationDTO shortUsernameDTO = new UserRegistrationDTO();
        shortUsernameDTO.setUsername("ab");
        shortUsernameDTO.setEmail("test@example.com");
        shortUsernameDTO.setPassword("password123");
        shortUsernameDTO.setFullName("Test User");

        // When/Then
        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(shortUsernameDTO)))
                .andExpect(status().isBadRequest());

        verify(userService, never()).registerUser(any());
    }

    @Test
    void registerUser_AutomaticallyAssignsUserRole() throws Exception {
        // Given
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                testUser, null, testUser.getAuthorities()
        );
        String mockToken = "mock.jwt.token";

        when(userService.registerUser(any(UserRegistrationDTO.class))).thenReturn(testUser);
        when(authenticationManager.authenticate(any(Authentication.class))).thenReturn(authentication);
        when(tokenProvider.generateToken(any(Authentication.class))).thenReturn(mockToken);

        // When/Then
        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registrationDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role", is("ROLE_USER")));
    }

    @Test
    void loginUser_GeneratesNewToken() throws Exception {
        // Given
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                testUser, null, testUser.getAuthorities()
        );
        String mockToken = "new.jwt.token";

        when(authenticationManager.authenticate(any(Authentication.class))).thenReturn(authentication);
        when(tokenProvider.generateToken(any(Authentication.class))).thenReturn(mockToken);
        when(userService.getUserByUsername(anyString())).thenReturn(testUser);

        // When/Then
        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", notNullValue()))
                .andExpect(jsonPath("$.token", is(mockToken)));
    }
}
