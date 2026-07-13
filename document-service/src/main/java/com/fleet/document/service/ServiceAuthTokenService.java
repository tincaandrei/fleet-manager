package com.fleet.document.service;

import com.fleet.document.config.JwtProperties;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;
import java.util.List;

/**
 * Mints short-lived service tokens for scheduled jobs, which run without a
 * user request context. Peer services validate JWTs statelessly against the
 * shared secret, so a SUPERADMIN-role token lets background jobs read
 * cross-organization data (e.g. business admin lookups).
 */
@Service
@RequiredArgsConstructor
public class ServiceAuthTokenService {

    private static final Duration TOKEN_TTL = Duration.ofMinutes(5);

    private final JwtProperties jwtProperties;

    public String mintServiceAuthorizationHeader() {
        Date now = new Date();
        String token = Jwts.builder()
                .setSubject("document-service")
                .claim("role", "SUPERADMIN")
                .claim("roles", List.of("SUPERADMIN"))
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + TOKEN_TTL.toMillis()))
                .signWith(
                        Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8)),
                        SignatureAlgorithm.HS256
                )
                .compact();
        return "Bearer " + token;
    }
}
