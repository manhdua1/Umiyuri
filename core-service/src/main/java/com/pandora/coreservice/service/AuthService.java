package com.pandora.coreservice.service;

import com.pandora.coreservice.dto.LoginRequest;
import com.pandora.coreservice.dto.LoginResponse;
import com.pandora.coreservice.entity.User;
import com.pandora.coreservice.exception.AppException;
import com.pandora.coreservice.exception.ErrorCode;
import com.pandora.coreservice.repo.UserRepository;
import com.pandora.coreservice.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_CREDENTIALS));

        String passwordHash = user.getPasswordHash();
        boolean enabled = user.isEnabled();

        if (!enabled || passwordHash == null || !passwordEncoder.matches(request.getPassword(), passwordHash)) {
            throw new AppException(ErrorCode.INVALID_CREDENTIALS);
        }

        String token = jwtService.generateToken(user);
        return new LoginResponse(token, jwtService.getTokenType(), jwtService.getExpiresIn());
    }
}
