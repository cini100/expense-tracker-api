package com.example.expensetracker.auth.service;

import com.example.expensetracker.auth.dto.AuthResponse;
import com.example.expensetracker.auth.dto.LoginRequest;
import com.example.expensetracker.auth.dto.RegisterRequest;
import com.example.expensetracker.user.entity.User;
import com.example.expensetracker.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Test
    void registerHashesPasswordSavesUserAndReturnsToken() {
        AuthService authService = new AuthService(passwordEncoder, userRepository, jwtService);
        when(userRepository.existsByEmail("user@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed");
        when(jwtService.generateToken(any())).thenReturn("jwt");
        when(jwtService.getExpirationInSeconds()).thenReturn(3600L);

        AuthResponse response = authService.register(new RegisterRequest("user@example.com", "password123"));

        assertThat(response.accessToken()).isEqualTo("jwt");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresInSeconds()).isEqualTo(3600L);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void registerDuplicateEmailReturnsConflict() {
        AuthService authService = new AuthService(passwordEncoder, userRepository, jwtService);
        when(userRepository.existsByEmail("user@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(new RegisterRequest("user@example.com", "password123")))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void loginReturnsTokenForMatchingPassword() {
        AuthService authService = new AuthService(passwordEncoder, userRepository, jwtService);
        User user = new User("user@example.com", "hashed");
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "hashed")).thenReturn(true);
        when(jwtService.generateToken(any())).thenReturn("jwt");
        when(jwtService.getExpirationInSeconds()).thenReturn(3600L);

        AuthResponse response = authService.login(new LoginRequest("user@example.com", "password123"));

        assertThat(response.accessToken()).isEqualTo("jwt");
    }

    @Test
    void loginWrongPasswordReturnsUnauthorized() {
        AuthService authService = new AuthService(passwordEncoder, userRepository, jwtService);
        User user = new User("user@example.com", "hashed");
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongpass", "hashed")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("user@example.com", "wrongpass")))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
