package com.fleet.fleet.service;

import com.fleet.fleet.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class JwtService {

    private static final String ROLES_CLAIM = "roles";
    private static final String ROLE_CLAIM = "role";
    private static final String USER_ID_CLAIM = "userId";
    private static final String BUSINESS_ID_CLAIM = "businessId";

    private final JwtProperties jwtProperties;

    public JwtPrincipal extractPrincipal(String token) {
        Claims claims = extractAllClaims(token);
        return new JwtPrincipal(
                claims.getSubject(),
                extractUserId(claims),
                extractBusinessId(claims),
                extractRoles(claims)
        );
    }

    public boolean validateToken(String token) {
        return StringUtils.hasText(extractClaim(token, Claims::getSubject)) && !isTokenExpired(token);
    }

    public Set<GrantedAuthority> toAuthorities(Set<String> roles) {
        return roles.stream()
                .flatMap(role -> {
                    if ("USER".equals(role)) {
                        return List.of("EMPLOYEE").stream();
                    }
                    if ("ADMIN".equals(role)) {
                        return List.of("SUPERADMIN").stream();
                    }
                    if ("STAFF".equals(role)) {
                        return List.of("BUSINESS_ADMIN").stream();
                    }
                    return List.of(role).stream();
                })
                .map(role -> "ROLE_" + role)
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toUnmodifiableSet());
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

    private Set<String> extractRoles(Claims claims) {
        Set<String> roles = new LinkedHashSet<>();
        Object rolesClaim = claims.get(ROLES_CLAIM);
        if (rolesClaim instanceof Collection<?> values) {
            values.stream()
                    .map(Object::toString)
                    .map(this::normalizeRole)
                    .filter(StringUtils::hasText)
                    .forEach(roles::add);
        } else if (rolesClaim instanceof String value && StringUtils.hasText(value)) {
            for (String role : value.split(",")) {
                Optional.of(normalizeRole(role)).filter(StringUtils::hasText).ifPresent(roles::add);
            }
        }

        Object roleClaim = claims.get(ROLE_CLAIM);
        if (roleClaim != null) {
            Optional.of(normalizeRole(roleClaim.toString())).filter(StringUtils::hasText).ifPresent(roles::add);
        }

        if (roles.isEmpty()) {
            throw new IllegalArgumentException("JWT is missing roles claim");
        }
        return Set.copyOf(roles);
    }

    private String normalizeRole(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (normalized.startsWith("ROLE_")) {
            normalized = normalized.substring(5);
        }
        return normalized;
    }

    private Long extractUserId(Claims claims) {
        Object claim = claims.get(USER_ID_CLAIM);
        if (claim instanceof Number number) {
            return number.longValue();
        }
        if (claim instanceof String value && StringUtils.hasText(value)) {
            try {
                return Long.parseLong(value);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        String subject = claims.getSubject();
        if (StringUtils.hasText(subject)) {
            try {
                return Long.parseLong(subject);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private Long extractBusinessId(Claims claims) {
        Object claim = claims.get(BUSINESS_ID_CLAIM);
        if (claim instanceof Number number) {
            return number.longValue();
        }
        if (claim instanceof String value && StringUtils.hasText(value)) {
            try {
                return Long.parseLong(value);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private boolean isTokenExpired(String token) {
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }

    private Key getSignKey() {
        return Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }
}
