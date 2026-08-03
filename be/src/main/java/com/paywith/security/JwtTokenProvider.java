package com.paywith.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
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
        return resolveStatus(token) == TokenStatus.VALID;
    }

    /** 만료(EXPIRED)와 그 외 무효(INVALID)를 구분해야 하는 곳(JwtAuthenticationFilter)에서 쓴다. */
    public TokenStatus resolveStatus(String token) {
        try {
            getClaims(token);
            return TokenStatus.VALID;
        } catch (ExpiredJwtException exception) {
            return TokenStatus.EXPIRED;
        } catch (RuntimeException exception) {
            return TokenStatus.INVALID;
        }
    }

    public enum TokenStatus {
        VALID,
        EXPIRED,
        INVALID
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
