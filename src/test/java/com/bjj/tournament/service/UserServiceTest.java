package com.bjj.tournament.service;

import com.bjj.tournament.dto.UserRegistrationDTO;
import com.bjj.tournament.entity.User;
import com.bjj.tournament.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for UserService
 * Tests user registration, authentication, and user management operations
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private UserRegistrationDTO registrationDTO;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword("encodedPassword");
        testUser.setFullName("Test User");
        testUser.setRole("ROLE_USER");
        testUser.setEnabled(true);
        testUser.setAccountNonLocked(true);

        registrationDTO = new UserRegistrationDTO();
        registrationDTO.setUsername("testuser");
        registrationDTO.setEmail("test@example.com");
        registrationDTO.setPassword("password123");
        registrationDTO.setFullName("Test User");
    }

    @Test
    void registerUser_WithValidData_CreatesNewUser() {
        // Given
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        User result = userService.registerUser(registrationDTO);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo("testuser");
        assertThat(result.getEmail()).isEqualTo("test@example.com");
        assertThat(result.getFullName()).isEqualTo("Test User");
        assertThat(result.getRole()).isEqualTo("ROLE_USER");
        assertThat(result.getEnabled()).isTrue();
        assertThat(result.getAccountNonLocked()).isTrue();

        verify(userRepository).existsByUsername("testuser");
        verify(userRepository).existsByEmail("test@example.com");
        verify(passwordEncoder).encode("password123");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void registerUser_WithDuplicateUsername_ThrowsException() {
        // Given
        when(userRepository.existsByUsername("testuser")).thenReturn(true);

        // When/Then
        assertThatThrownBy(() -> userService.registerUser(registrationDTO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Username 'testuser' is already taken");

        verify(userRepository).existsByUsername("testuser");
        verify(userRepository, never()).save(any());
    }

    @Test
    void registerUser_WithDuplicateEmail_ThrowsException() {
        // Given
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        // When/Then
        assertThatThrownBy(() -> userService.registerUser(registrationDTO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Email 'test@example.com' is already registered");

        verify(userRepository).existsByUsername("testuser");
        verify(userRepository).existsByEmail("test@example.com");
        verify(userRepository, never()).save(any());
    }

    @Test
    void registerUser_EncodesPassword() {
        // Given
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        userService.registerUser(registrationDTO);

        // Then
        verify(passwordEncoder).encode("password123");
    }

    @Test
    void registerUser_AssignsDefaultUserRole() {
        // Given
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User savedUser = invocation.getArgument(0);
            assertThat(savedUser.getRole()).isEqualTo("ROLE_USER");
            return savedUser;
        });

        // When
        userService.registerUser(registrationDTO);

        // Then
        verify(userRepository).save(argThat(user ->
                user.getRole().equals("ROLE_USER")
        ));
    }

    @Test
    void registerUser_EnablesAccountByDefault() {
        // Given
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User savedUser = invocation.getArgument(0);
            assertThat(savedUser.getEnabled()).isTrue();
            assertThat(savedUser.getAccountNonLocked()).isTrue();
            return savedUser;
        });

        // When
        userService.registerUser(registrationDTO);

        // Then
        verify(userRepository).save(argThat(user ->
                user.getEnabled() && user.getAccountNonLocked()
        ));
    }

    @Test
    void getUserByUsername_WhenUserExists_ReturnsUser() {
        // Given
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // When
        User result = userService.getUserByUsername("testuser");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo("testuser");
        verify(userRepository).findByUsername("testuser");
    }

    @Test
    void getUserByUsername_WhenUserNotFound_ThrowsException() {
        // Given
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> userService.getUserByUsername("nonexistent"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User not found with username: nonexistent");

        verify(userRepository).findByUsername("nonexistent");
    }

    @Test
    void getUserById_WhenUserExists_ReturnsUser() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // When
        User result = userService.getUserById(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        verify(userRepository).findById(1L);
    }

    @Test
    void getUserById_WhenUserNotFound_ThrowsException() {
        // Given
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> userService.getUserById(999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User not found with ID: 999");

        verify(userRepository).findById(999L);
    }

    @Test
    void getAllUsers_ReturnsListOfUsers() {
        // Given
        User user2 = new User();
        user2.setId(2L);
        user2.setUsername("user2");
        user2.setEmail("user2@example.com");

        when(userRepository.findAll()).thenReturn(Arrays.asList(testUser, user2));

        // When
        List<User> results = userService.getAllUsers();

        // Then
        assertThat(results).hasSize(2);
        assertThat(results.get(0).getUsername()).isEqualTo("testuser");
        assertThat(results.get(1).getUsername()).isEqualTo("user2");
        verify(userRepository).findAll();
    }

    @Test
    void updateUserRole_UpdatesRoleSuccessfully() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        User result = userService.updateUserRole(1L, "ROLE_ADMIN");

        // Then
        assertThat(result.getRole()).isEqualTo("ROLE_ADMIN");
        verify(userRepository).findById(1L);
        verify(userRepository).save(testUser);
    }

    @Test
    void updateUserStatus_EnablesUserSuccessfully() {
        // Given
        testUser.setEnabled(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        User result = userService.updateUserStatus(1L, true);

        // Then
        assertThat(result.getEnabled()).isTrue();
        verify(userRepository).findById(1L);
        verify(userRepository).save(testUser);
    }

    @Test
    void updateUserStatus_DisablesUserSuccessfully() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        User result = userService.updateUserStatus(1L, false);

        // Then
        assertThat(result.getEnabled()).isFalse();
        verify(userRepository).findById(1L);
        verify(userRepository).save(testUser);
    }

    @Test
    void deleteUser_DeletesUserSuccessfully() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        doNothing().when(userRepository).delete(testUser);

        // When
        userService.deleteUser(1L);

        // Then
        verify(userRepository).findById(1L);
        verify(userRepository).delete(testUser);
    }

    @Test
    void deleteUser_WhenUserNotFound_ThrowsException() {
        // Given
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> userService.deleteUser(999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User not found with ID: 999");

        verify(userRepository).findById(999L);
        verify(userRepository, never()).delete(any());
    }

    @Test
    void registerUser_StoresAllUserInformation() {
        // Given
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User savedUser = invocation.getArgument(0);
            assertThat(savedUser.getUsername()).isEqualTo("testuser");
            assertThat(savedUser.getEmail()).isEqualTo("test@example.com");
            assertThat(savedUser.getFullName()).isEqualTo("Test User");
            assertThat(savedUser.getPassword()).isEqualTo("encodedPassword");
            return savedUser;
        });

        // When
        userService.registerUser(registrationDTO);

        // Then
        verify(userRepository).save(any(User.class));
    }
}
