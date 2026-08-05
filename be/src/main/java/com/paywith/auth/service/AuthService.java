package com.paywith.auth.service;

import com.paywith.auth.dto.LoginRequest;
import com.paywith.auth.dto.RefreshTokenRequest;
import com.paywith.auth.dto.TokenResponse;
import com.paywith.common.PhoneNumberNormalizer;
import com.paywith.exception.BusinessException;
import com.paywith.security.JwtTokenProvider;
import com.paywith.user.domain.User;
import com.paywith.user.domain.UserStatus;
import com.paywith.user.mapper.UserMapper;
import java.time.Duration;
import org.springframework.http.HttpStatus;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserMapper userMapper;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final RedisTemplate<String, String> redisTemplate;

    public AuthService(
        UserMapper userMapper,
        JwtTokenProvider jwtTokenProvider,
        PasswordEncoder passwordEncoder,
        RedisTemplate<String, String> redisTemplate
    ) {
        this.userMapper = userMapper;
        this.jwtTokenProvider = jwtTokenProvider;
        this.passwordEncoder = passwordEncoder;
        this.redisTemplate = redisTemplate;

    }

    public TokenResponse login(LoginRequest request) {
        String phone = PhoneNumberNormalizer.normalize(request.getPhone());
        User user = userMapper.findByPhone(phone);
        if (
            user == null
            || user.getStatus() == UserStatus.WITHDRAWN
            || !matchesPassword(request.getPassword(), user.getPassword())
        ) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "전화번호 또는 비밀번호가 올바르지 않습니다.");
        }

        String accessToken = jwtTokenProvider.createAccessToken(user.getId(), user.getPhone());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId(), user.getPhone());
        redisTemplate.opsForValue().set("refresh:" + user.getId(), refreshToken, Duration.ofMillis(jwtTokenProvider.getRefreshTokenValidityMs()));
        return new TokenResponse(accessToken, refreshToken);
    }

    public TokenResponse refresh(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "리프레시 토큰이 유효하지 않습니다.");
        }

        Long userId = jwtTokenProvider.getUserId(refreshToken);
        String savedRefreshToken = redisTemplate.opsForValue().get("refresh:" + userId);
        if (!refreshToken.equals(savedRefreshToken)) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "리프레시 토큰이 일치하지 않습니다.");
        }

        User user = userMapper.findById(userId);
        if (user == null || user.getStatus() == UserStatus.WITHDRAWN) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "사용자를 찾을 수 없습니다.");
        }

        String newAccessToken = jwtTokenProvider.createAccessToken(user.getId(), user.getPhone());
        String newRefreshToken = jwtTokenProvider.createRefreshToken(user.getId(), user.getPhone());
        redisTemplate.opsForValue().set("refresh:" + user.getId(), newRefreshToken, Duration.ofMillis(jwtTokenProvider.getRefreshTokenValidityMs()));
        return new TokenResponse(newAccessToken, newRefreshToken);
    }


    private boolean matchesPassword(String rawPassword, String savedPassword) {
        return passwordEncoder.matches(rawPassword, savedPassword);

    }
}
