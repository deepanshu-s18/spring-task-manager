package com.deepanshu.taskmanager.service;

import com.deepanshu.taskmanager.dto.request.AuthRequest;
import com.deepanshu.taskmanager.dto.response.ApiResponse;
import com.deepanshu.taskmanager.exception.ConflictException;
import com.deepanshu.taskmanager.model.User;
import com.deepanshu.taskmanager.repository.UserRepository;
import com.deepanshu.taskmanager.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Unit Tests")
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private UserDetailsService userDetailsService;

    @InjectMocks private AuthService authService;

    private AuthRequest.Register registerRequest;
    private AuthRequest.Login loginRequest;
    private User savedUser;

    @BeforeEach
    void setUp() {
        registerRequest = AuthRequest.Register.builder()
            .username("deepanshu")
            .email("deepanshuk2555@gmail.com")
            .password("Test@1234")
            .fullName("Deepanshu Singh")
            .build();

        loginRequest = AuthRequest.Login.builder()
            .usernameOrEmail("deepanshu")
            .password("Test@1234")
            .build();

        savedUser = User.builder()
            .id(1L)
            .username("deepanshu")
            .email("deepanshuk2555@gmail.com")
            .password("$2a$12$encodedpassword")
            .fullName("Deepanshu Singh")
            .role(User.Role.USER)
            .build();
    }

    @Test
    @DisplayName("register() - success: new user is saved and tokens returned")
    void register_newUser_returnsTokens() {
        when(userRepository.existsByUsername("deepanshu")).thenReturn(false);
        when(userRepository.existsByEmail("deepanshuk2555@gmail.com")).thenReturn(false);
        when(passwordEncoder.encode("Test@1234")).thenReturn("$2a$12$encoded");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(authenticationManager.authenticate(any())).thenReturn(mock(Authentication.class));
        when(jwtTokenProvider.generateToken(any())).thenReturn("access-token");
        when(jwtTokenProvider.generateRefreshToken(any())).thenReturn("refresh-token");

        ApiResponse.AuthTokens result = authService.register(registerRequest);

        assertThat(result).isNotNull();
        assertThat(result.getAccessToken()).isEqualTo("access-token");
        assertThat(result.getRefreshToken()).isEqualTo("refresh-token");
        assertThat(result.getUser().getUsername()).isEqualTo("deepanshu");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("register() - conflict: duplicate username throws ConflictException")
    void register_duplicateUsername_throwsConflict() {
        when(userRepository.existsByUsername("deepanshu")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(registerRequest))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("deepanshu");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("register() - conflict: duplicate email throws ConflictException")
    void register_duplicateEmail_throwsConflict() {
        when(userRepository.existsByUsername("deepanshu")).thenReturn(false);
        when(userRepository.existsByEmail("deepanshuk2555@gmail.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(registerRequest))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("deepanshuk2555@gmail.com");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("login() - success: valid credentials return tokens")
    void login_validCredentials_returnsTokens() {
        Authentication auth = mock(Authentication.class);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(auth);
        when(userRepository.findByUsername("deepanshu")).thenReturn(Optional.of(savedUser));
        when(jwtTokenProvider.generateToken(auth)).thenReturn("access-token");
        when(jwtTokenProvider.generateRefreshToken("deepanshu")).thenReturn("refresh-token");

        ApiResponse.AuthTokens result = authService.login(loginRequest);

        assertThat(result.getAccessToken()).isEqualTo("access-token");
        assertThat(result.getUser().getEmail()).isEqualTo("deepanshuk2555@gmail.com");
    }

    @Test
    @DisplayName("login() - failure: bad credentials throws BadCredentialsException")
    void login_badCredentials_throws() {
        when(authenticationManager.authenticate(any()))
            .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.login(loginRequest))
            .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    @DisplayName("login() - email login: user found by email, login succeeds")
    void login_withEmail_resolvesUsernameAndReturnsTokens() {
        AuthRequest.Login emailLogin = AuthRequest.Login.builder()
            .usernameOrEmail("deepanshuk2555@gmail.com")
            .password("Test@1234")
            .build();

        when(userRepository.findByEmail("deepanshuk2555@gmail.com")).thenReturn(Optional.of(savedUser));
        when(authenticationManager.authenticate(any())).thenReturn(mock(Authentication.class));
        when(userRepository.findByUsername("deepanshu")).thenReturn(Optional.of(savedUser));
        when(jwtTokenProvider.generateToken(any())).thenReturn("access-token");
        when(jwtTokenProvider.generateRefreshToken(any())).thenReturn("refresh-token");

        ApiResponse.AuthTokens result = authService.login(emailLogin);
        assertThat(result.getUser().getUsername()).isEqualTo("deepanshu");
    }
}
