package com.paywith.auth.service;

import com.paywith.auth.dto.LoginRequest;
import com.paywith.auth.dto.PasswordResetRequest;
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

    private static final String PHONE_PATTERN = "01[016789]\\d{7,8}";

    private final UserMapper userMapper;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final RedisTemplate<String, String> redisTemplate;
    private final PhoneVerificationService phoneVerificationService;

    public AuthService(
        UserMapper userMapper,
        JwtTokenProvider jwtTokenProvider,
        PasswordEncoder passwordEncoder,
        RedisTemplate<String, String> redisTemplate,
        PhoneVerificationService phoneVerificationService
    ) {
        this.userMapper = userMapper;
        this.jwtTokenProvider = jwtTokenProvider;
        this.passwordEncoder = passwordEncoder;
        this.redisTemplate = redisTemplate;
        this.phoneVerificationService = phoneVerificationService;

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


    public void resetPassword(PasswordResetRequest request) {
        String phone = PhoneNumberNormalizer.normalize(request.getPhone());
        if (phone == null || !phone.matches(PHONE_PATTERN)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "PHONE_001", "잘못된 형식으로 입력하였습니다.");
        }

        String newPassword = request.getNewPassword();
        if (newPassword == null || newPassword.length() < 8) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "PASSWORD_001", "비밀번호는 8자 이상이어야 합니다.");
        }

        phoneVerificationService.requireValidToken(request.getVerificationToken(), phone);

        User user = userMapper.findByPhone(phone);
        if (user == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "USER_002", "가입되지 않은 전화번호입니다.");
        }
        userMapper.updatePassword(user.getId(), passwordEncoder.encode(newPassword));
        redisTemplate.delete("refresh:" + user.getId());

        phoneVerificationService.invalidateToken(request.getVerificationToken(), phone);
    }

    private boolean matchesPassword(String rawPassword, String savedPassword) {
        return passwordEncoder.matches(rawPassword, savedPassword);

    }
}
