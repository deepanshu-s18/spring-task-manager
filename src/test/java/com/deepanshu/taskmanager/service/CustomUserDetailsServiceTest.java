package com.deepanshu.taskmanager.service;

import com.deepanshu.taskmanager.model.User;
import com.deepanshu.taskmanager.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CustomUserDetailsService Unit Tests")
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService userDetailsService;

    private User activeUser;
    private User inactiveUser;

    @BeforeEach
    void setUp() {
        activeUser = User.builder()
            .id(1L)
            .username("deepanshu")
            .password("encodedPassword")
            .email("deepanshuk2555@gmail.com")
            .role(User.Role.USER)
            .isActive(true)
            .build();

        inactiveUser = User.builder()
            .id(2L)
            .username("inactiveUser")
            .password("encodedPassword")
            .email("inactive@example.com")
            .role(User.Role.ADMIN)
            .isActive(false)
            .build();
    }

    @Test
    @DisplayName("loadUserByUsername - returns UserDetails when active user exists")
    void loadUserByUsername_Success() {
        when(userRepository.findActiveByUsername("deepanshu")).thenReturn(Optional.of(activeUser));

        UserDetails userDetails = userDetailsService.loadUserByUsername("deepanshu");

        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo("deepanshu");
        assertThat(userDetails.getPassword()).isEqualTo("encodedPassword");
        assertThat(userDetails.isEnabled()).isTrue();
        assertThat(userDetails.getAuthorities())
            .extracting("authority")
            .containsExactly("ROLE_USER");
    }

    @Test
    @DisplayName("loadUserByUsername - throws UsernameNotFoundException when user does not exist")
    void loadUserByUsername_NotFound_ThrowsException() {
        when(userRepository.findActiveByUsername("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("unknown"))
            .isInstanceOf(UsernameNotFoundException.class)
            .hasMessageContaining("User not found: unknown");
    }

    @Test
    @DisplayName("loadUserByUsername - loads inactive user with isEnabled false")
    void loadUserByUsername_InactiveUser() {
        when(userRepository.findActiveByUsername("inactiveUser")).thenReturn(Optional.of(inactiveUser));

        UserDetails userDetails = userDetailsService.loadUserByUsername("inactiveUser");

        assertThat(userDetails).isNotNull();
        assertThat(userDetails.isEnabled()).isFalse();
        assertThat(userDetails.getAuthorities())
            .extracting("authority")
            .containsExactly("ROLE_ADMIN");
    }
}
