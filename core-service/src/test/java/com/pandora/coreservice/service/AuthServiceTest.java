package com.pandora.coreservice.service;

import com.pandora.coreservice.dto.LoginRequest;
import com.pandora.coreservice.dto.LoginResponse;
import com.pandora.coreservice.entity.TestUserFactory;
import com.pandora.coreservice.entity.User;
import com.pandora.coreservice.exception.AppException;
import com.pandora.coreservice.exception.ErrorCode;
import com.pandora.coreservice.repo.UserRepository;
import com.pandora.coreservice.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
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

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, passwordEncoder, jwtService);
    }

    @Test
    @DisplayName("login succeeds for valid credentials of enabled user")
    void testLoginSuccess() {
        UUID id = UUID.randomUUID();
        User user = TestUserFactory.createDefaultUser(id, "valid_user", "$2a$10$hashed_password");

        when(userRepository.findByUsername("valid_user")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("raw_secret_pw", "$2a$10$hashed_password")).thenReturn(true);
        when(jwtService.generateToken(user)).thenReturn("mock.jwt.token");
        when(jwtService.getTokenType()).thenReturn("Bearer");
        when(jwtService.getExpiresIn()).thenReturn(3600L);

        LoginRequest request = new LoginRequest("valid_user", "raw_secret_pw");
        LoginResponse response = authService.login(request);

        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("mock.jwt.token");
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        assertThat(response.getExpiresIn()).isEqualTo(3600L);

        verify(jwtService).generateToken(user);
    }

    @Test
    @DisplayName("login fails with generic 401 when username is not found")
    void testLoginUnknownUser() {
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        LoginRequest request = new LoginRequest("nonexistent", "some_password");

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(AppException.class)
                .hasMessage("Invalid credentials")
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode()).isEqualTo(ErrorCode.INVALID_CREDENTIALS));

        verify(jwtService, never()).generateToken(any());
    }

    @Test
    @DisplayName("login fails with generic 401 when password does not match")
    void testLoginBadPassword() {
        UUID id = UUID.randomUUID();
        User user = TestUserFactory.createDefaultUser(id, "valid_user", "$2a$10$hashed_password");

        when(userRepository.findByUsername("valid_user")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong_pw", "$2a$10$hashed_password")).thenReturn(false);

        LoginRequest request = new LoginRequest("valid_user", "wrong_pw");

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(AppException.class)
                .hasMessage("Invalid credentials")
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode()).isEqualTo(ErrorCode.INVALID_CREDENTIALS));

        verify(jwtService, never()).generateToken(any());
    }

    @Test
    @DisplayName("login fails with generic 401 when user is disabled")
    void testLoginDisabledUser() {
        UUID id = UUID.randomUUID();
        User disabledUser = TestUserFactory.createDisabledUser(id, "disabled_user", "$2a$10$hashed_password");

        when(userRepository.findByUsername("disabled_user")).thenReturn(Optional.of(disabledUser));

        LoginRequest request = new LoginRequest("disabled_user", "any_password");

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(AppException.class)
                .hasMessage("Invalid credentials")
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode()).isEqualTo(ErrorCode.INVALID_CREDENTIALS));

        verify(jwtService, never()).generateToken(any());
    }
}
