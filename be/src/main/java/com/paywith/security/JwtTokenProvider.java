package com.paywith.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.security.Key;
import java.util.Date;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider {

    private final Key signingKey;
    private final long accessTokenValidityMs;
    private final long refreshTokenValidityMs;

    public JwtTokenProvider(
        @Value("${jwt.secret}") String secret,
        @Value("${jwt.access-token-validity-ms}") long accessTokenValidityMs,
        @Value("${jwt.refresh-token-validity-ms}") long refreshTokenValidityMs
    ) {
        this.signingKey = Keys.hmacShaKeyFor(toKeyBytes(secret));
        this.accessTokenValidityMs = accessTokenValidityMs;
        this.refreshTokenValidityMs = refreshTokenValidityMs;
    }

    public long getRefreshTokenValidityMs() {
        return refreshTokenValidityMs;
    }

    public String createAccessToken(Long userId, String phone) {
        return createToken(userId, phone, accessTokenValidityMs);
    }

    public String createRefreshToken(Long userId, String phone) {
        return createToken(userId, phone, refreshTokenValidityMs);
    }

    public boolean validateToken(String token) {
        try {
            getClaims(token);
            return true;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    public Long getUserId(String token) {
        return Long.valueOf(getClaims(token).getSubject());
    }

    public String getPhone(String token) {
        return getClaims(token).get("phone", String.class);
    }

    private String createToken(Long userId, String phone, long validityMs) {
        Date now = new Date();
        Date expiresAt = new Date(now.getTime() + validityMs);

        return Jwts.builder()
            .setSubject(String.valueOf(userId))
            .claim("phone", phone)
            .setIssuedAt(now)
            .setExpiration(expiresAt)
            .signWith(signingKey, SignatureAlgorithm.HS256)
            .compact();
    }

    private Claims getClaims(String token) {
        return Jwts.parserBuilder()
            .setSigningKey(signingKey)
            .build()
            .parseClaimsJws(token)
            .getBody();
    }

    private byte[] toKeyBytes(String secret) {
        try {
            return Decoders.BASE64.decode(secret);
        } catch (RuntimeException exception) {
            return secret.getBytes();
        }
    }
}
