package com.pandora.coreservice.security;

import com.pandora.coreservice.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class JwtService {

    public static final long TOKEN_EXPIRY_SECONDS = 3600L;
    public static final String TOKEN_TYPE = "Bearer";

    private final JwtEncoder jwtEncoder;

    public String generateToken(User user) {
        Objects.requireNonNull(user, "User cannot be null");
        UUID userId = user.getId();
        Objects.requireNonNull(userId, "User ID cannot be null");

        Instant now = Instant.now();
        JwtClaimsSet.Builder claimsBuilder = JwtClaimsSet.builder()
                .subject(userId.toString())
                .issuedAt(now)
                .expiresAt(now.plusSeconds(TOKEN_EXPIRY_SECONDS));

        if (user.getRole() != null) {
            claimsBuilder.claim("role", user.getRole().name());
        }

        JwtClaimsSet claims = claimsBuilder.build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return this.jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    public long getExpiresIn() {
        return TOKEN_EXPIRY_SECONDS;
    }

    public String getTokenType() {
        return TOKEN_TYPE;
    }
}
