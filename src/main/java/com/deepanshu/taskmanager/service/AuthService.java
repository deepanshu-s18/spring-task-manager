package com.deepanshu.taskmanager.service;

import com.deepanshu.taskmanager.dto.request.AuthRequest;
import com.deepanshu.taskmanager.dto.response.ApiResponse;
import com.deepanshu.taskmanager.exception.ConflictException;
import com.deepanshu.taskmanager.exception.InvalidTokenException;
import com.deepanshu.taskmanager.model.User;
import com.deepanshu.taskmanager.repository.UserRepository;
import com.deepanshu.taskmanager.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;

    @Transactional
    public ApiResponse.AuthTokens register(AuthRequest.Register request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new ConflictException("Username '" + request.getUsername() + "' is already taken");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("Email '" + request.getEmail() + "' is already registered");
        }

        User user = User.builder()
            .username(request.getUsername())
            .email(request.getEmail())
            .password(passwordEncoder.encode(request.getPassword()))
            .fullName(request.getFullName())
            .role(User.Role.USER)
            .build();

        user = userRepository.save(user);
        log.info("Registered new user: {} ({})", user.getUsername(), user.getEmail());

        // Auto-login after registration
        Authentication auth = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        return buildTokenResponse(auth, user);
    }

    public ApiResponse.AuthTokens login(AuthRequest.Login request) {
        // Support login by username or email
        String username = request.getUsernameOrEmail();
        if (username.contains("@")) {
            username = userRepository.findByEmail(request.getUsernameOrEmail())
                .map(User::getUsername)
                .orElse(request.getUsernameOrEmail());
        }

        Authentication auth = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(username, request.getPassword())
        );

        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("User not found after authentication"));

        log.info("User '{}' logged in", username);
        return buildTokenResponse(auth, user);
    }

    public ApiResponse.AuthTokens refresh(AuthRequest.RefreshToken request) {
        String refreshToken = request.getRefreshToken();

        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new InvalidTokenException("Invalid or expired refresh token");
        }

        String username = jwtTokenProvider.extractUsername(refreshToken);
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new InvalidTokenException("User not found"));

        String newAccessToken = jwtTokenProvider.generateTokenFromUsername(username);
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(username);

        log.info("Refreshed tokens for user '{}'", username);

        return ApiResponse.AuthTokens.builder()
            .accessToken(newAccessToken)
            .refreshToken(newRefreshToken)
            .expiresIn(86400L)
            .user(ApiResponse.UserSummary.from(user))
            .build();
    }

    private ApiResponse.AuthTokens buildTokenResponse(Authentication auth, User user) {
        String accessToken = jwtTokenProvider.generateToken(auth);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getUsername());

        return ApiResponse.AuthTokens.builder()
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .expiresIn(86400L)
            .user(ApiResponse.UserSummary.from(user))
            .build();
    }
}
