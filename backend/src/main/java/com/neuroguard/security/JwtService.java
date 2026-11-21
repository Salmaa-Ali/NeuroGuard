package com.neuroguard.security;

import com.neuroguard.staff.entity.PermissionScope;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
public class JwtService {

    public record IssuedToken(String value, long expiresInSeconds) {
    }

    private final JwtEncoder encoder;
    private final JwtProperties properties;

    public JwtService(JwtEncoder encoder, JwtProperties properties) {
        this.encoder = encoder;
        this.properties = properties;
    }

    public IssuedToken issue(UserType userType, UUID userId, Role role, PermissionScope scope) {
        Instant now = Instant.now();
        Duration ttl = Duration.ofMinutes(properties.expiryMinutes());
        JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
                .issuer(properties.issuer())
                .subject(userId.toString())
                .issuedAt(now)
                .expiresAt(now.plus(ttl))
                .claim(CurrentUser.CLAIM_USER_TYPE, userType.name())
                .claim(CurrentUser.CLAIM_ROLE, role.name());
        if (scope != null) {
            claims.claim(CurrentUser.CLAIM_PERM_SCOPE, scope.name());
        }
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = encoder.encode(JwtEncoderParameters.from(header, claims.build())).getTokenValue();
        return new IssuedToken(token, ttl.toSeconds());
    }
}
