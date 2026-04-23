package com.hfnew.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Component
public class JwtService {
    private final String accessSecret;
    private final String refreshSecret;
    private final long accessTtlSeconds;
    private final long refreshTtlSeconds;
    private final String issuer;

    public JwtService(
            @Value("${app.jwt.access-secret}") String accessSecret,
            @Value("${app.jwt.refresh-secret}") String refreshSecret,
            @Value("${app.jwt.access-ttl-seconds}") long accessTtlSeconds,
            @Value("${app.jwt.refresh-ttl-seconds}") long refreshTtlSeconds,
            @Value("${app.jwt.issuer}") String issuer
    ) {
        this.accessSecret = accessSecret;
        this.refreshSecret = refreshSecret;
        this.accessTtlSeconds = accessTtlSeconds;
        this.refreshTtlSeconds = refreshTtlSeconds;
        this.issuer = issuer;
    }

    public long getAccessTtlSeconds() {
        return accessTtlSeconds;
    }

    public long getRefreshTtlSeconds() {
        return refreshTtlSeconds;
    }

    public String createAccessToken(Long userId, String username, List<String> permissions, List<String> roles, Long orgId) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + accessTtlSeconds * 1000);

        SecretKey key = Keys.hmacShaKeyFor(accessSecret.getBytes(StandardCharsets.UTF_8));

        Map<String, Object> claimsMap = new java.util.LinkedHashMap<>();
        claimsMap.put("username", username);
        claimsMap.put("perms", permissions);
        claimsMap.put("roles", roles);
        if (orgId != null) {
            claimsMap.put("orgId", orgId);
        }

        return Jwts.builder()
                .issuer(issuer)
                .issuedAt(now)
                .expiration(exp)
                .subject(String.valueOf(userId))
                .claims(claimsMap)
                .signWith(key)
                .compact();
    }

    public String createRefreshToken(Long userId, String username, List<String> permissions, List<String> roles, Long orgId) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + refreshTtlSeconds * 1000);

        SecretKey key = Keys.hmacShaKeyFor(refreshSecret.getBytes(StandardCharsets.UTF_8));

        Map<String, Object> claimsMap = new java.util.LinkedHashMap<>();
        claimsMap.put("username", username);
        claimsMap.put("perms", permissions);
        claimsMap.put("roles", roles);
        if (orgId != null) {
            claimsMap.put("orgId", orgId);
        }

        return Jwts.builder()
                .issuer(issuer)
                .issuedAt(now)
                .expiration(exp)
                .subject(String.valueOf(userId))
                .claims(claimsMap)
                .signWith(key)
                .compact();
    }

    public Claims parseAccessToken(String token) {
        SecretKey key = Keys.hmacShaKeyFor(accessSecret.getBytes(StandardCharsets.UTF_8));
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public Claims parseRefreshToken(String token) {
        SecretKey key = Keys.hmacShaKeyFor(refreshSecret.getBytes(StandardCharsets.UTF_8));
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}

