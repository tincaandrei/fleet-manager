package com.fleet.auth.service;

import com.fleet.auth.config.JwtProperties;
import com.fleet.auth.entity.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class JwtService {

    private static final String ROLE_CLAIM = "role";

    private final JwtProperties jwtProperties;

    public String generateToken(UserDetails userDetails) {
        Role role = userDetails instanceof CredentialDetails credentialDetails
                ? credentialDetails.getRole()
                : Role.fromAuthorities(userDetails.getAuthorities()).stream().findFirst().orElseThrow();
        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put(ROLE_CLAIM, role.name());
        claims.put("roles", List.of(role.name()));
        if (userDetails instanceof CredentialDetails credentialDetails && credentialDetails.getUserId() != null) {
            claims.put("userId", credentialDetails.getUserId());
        }
        if (userDetails instanceof CredentialDetails credentialDetails && credentialDetails.getBusinessId() != null) {
            claims.put("businessId", credentialDetails.getBusinessId());
            claims.put("businessName", credentialDetails.getBusinessName());
        }

        return Jwts.builder()
                .addClaims(claims)
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + jwtProperties.getExpirationMs()))
                .signWith(getSignKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Role extractRole(String token) {
        Object claim = extractAllClaims(token).get(ROLE_CLAIM);
        if (claim == null) {
            throw new IllegalArgumentException("JWT is missing role claim");
        }
        return Role.fromValue(claim.toString());
    }

    public boolean validateToken(String token) {
        return StringUtils.hasText(extractUsername(token)) && !isTokenExpired(token);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSignKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private boolean isTokenExpired(String token) {
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }

    private Key getSignKey() {
        return Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }
}
