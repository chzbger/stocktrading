package com.example.stocktrading.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Slf4j
@Service
public class JwtService {

    private final SecretKey secretKey;
    private final long expirationMs;

    public JwtService(
            @Value("${jwt.secret:96a2e55e838e278fb97a40403bb7d1122534eed1faf88f79a25b01827b379b9c}") String secret,
            @Value("${jwt.expiration-hours:24}") int expirationHours) {
        String paddedSecret = secret;
        while (paddedSecret.getBytes(StandardCharsets.UTF_8).length < 32) {
            paddedSecret = paddedSecret + secret;
        }
        this.secretKey = Keys.hmacShaKeyFor(paddedSecret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = (long) expirationHours * 60 * 60 * 1000;
        log.info("[JWT] Service initialized with {}h token expiration", expirationHours);
    }

    public String generateToken(Long userId, String role) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        String token = Jwts.builder()
                .subject(userId.toString())
                .claim("role", role)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(secretKey)
                .compact();

        log.debug("[JWT] Generated token for userId={}, expires={}", userId, expiry);
        return token;
    }

    public AuthContext.AuthInfo validateToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            Long userId = Long.parseLong(claims.getSubject());
            String role = claims.get("role", String.class);

            return new AuthContext.AuthInfo(userId, role);
        } catch (Exception e) {
            log.debug("[JWT] Token validation failed ", e);
        }
        return null;
    }
}
