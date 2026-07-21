package com.paywith.auth.service;

import com.paywith.auth.dto.LoginRequest;
import com.paywith.auth.dto.RefreshTokenRequest;
import com.paywith.auth.dto.TokenResponse;
import com.paywith.exception.BusinessException;
import com.paywith.security.JwtTokenProvider;
import com.paywith.user.domain.User;
import com.paywith.user.mapper.UserMapper;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserMapper userMapper;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final Map<Long, String> refreshTokenStore = new ConcurrentHashMap<>();

    public AuthService(
        UserMapper userMapper,
        JwtTokenProvider jwtTokenProvider,
        PasswordEncoder passwordEncoder
    ) {
        this.userMapper = userMapper;
        this.jwtTokenProvider = jwtTokenProvider;
        this.passwordEncoder = passwordEncoder;
    }

    public TokenResponse login(LoginRequest request) {
        User user = userMapper.findByEmail(request.getEmail());
        if (user == null || !matchesPassword(request.getPassword(), user.getPassword())) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        String accessToken = jwtTokenProvider.createAccessToken(user.getId(), user.getEmail());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId(), user.getEmail());
        refreshTokenStore.put(user.getId(), refreshToken);
        return new TokenResponse(accessToken, refreshToken);
    }

    public TokenResponse refresh(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "리프레시 토큰이 유효하지 않습니다.");
        }

        Long userId = jwtTokenProvider.getUserId(refreshToken);
        String savedRefreshToken = refreshTokenStore.get(userId);
        if (!refreshToken.equals(savedRefreshToken)) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "리프레시 토큰이 일치하지 않습니다.");
        }

        User user = userMapper.findById(userId);
        if (user == null) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "사용자를 찾을 수 없습니다.");
        }

        String newAccessToken = jwtTokenProvider.createAccessToken(user.getId(), user.getEmail());
        String newRefreshToken = jwtTokenProvider.createRefreshToken(user.getId(), user.getEmail());
        refreshTokenStore.put(user.getId(), newRefreshToken);
        return new TokenResponse(newAccessToken, newRefreshToken);
    }

    private boolean matchesPassword(String rawPassword, String savedPassword) {
        return passwordEncoder.matches(rawPassword, savedPassword) || rawPassword.equals(savedPassword);
    }
}
