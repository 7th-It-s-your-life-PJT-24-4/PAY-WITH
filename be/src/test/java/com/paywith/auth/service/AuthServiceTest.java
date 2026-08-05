package com.paywith.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;

import com.paywith.auth.dto.LoginRequest;
import com.paywith.auth.dto.RefreshTokenRequest;
import com.paywith.auth.dto.TokenResponse;
import com.paywith.exception.BusinessException;
import com.paywith.security.JwtTokenProvider;
import com.paywith.user.domain.Role;
import com.paywith.user.domain.User;
import com.paywith.user.domain.UserStatus;
import com.paywith.user.mapper.UserMapper;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
@DisplayName("인증 서비스")
class AuthServiceTest {

    private static final Long USER_ID = 1L;
    private static final String PHONE = "01012345678";
    private static final long REFRESH_TTL_MS = 1_000_000L;

    @Mock
    private UserMapper userMapper;
    @Mock
    private JwtTokenProvider jwtTokenProvider;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private RedisTemplate<String, String> redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        authService = new AuthService(userMapper, jwtTokenProvider, passwordEncoder, redisTemplate);
    }

    private User user() {
        User user = new User();
        user.setId(USER_ID);
        user.setRole(Role.WARD);
        user.setPhone(PHONE);
        user.setPassword("encoded-password");
        return user;
    }

    private LoginRequest loginRequest() {
        LoginRequest request = new LoginRequest();
        request.setPhone(PHONE);
        request.setPassword("raw-password");
        return request;
    }

    @Nested
    @DisplayName("login")
    class Login {

        @Test
        @DisplayName("정상 로그인이면 토큰을 발급하고 refresh token을 Redis에 저장한다")
        void success() {
            given(userMapper.findByPhone(PHONE)).willReturn(user());
            given(passwordEncoder.matches("raw-password", "encoded-password")).willReturn(true);
            given(jwtTokenProvider.createAccessToken(USER_ID, PHONE)).willReturn("access-token");
            given(jwtTokenProvider.createRefreshToken(USER_ID, PHONE)).willReturn("refresh-token");
            given(jwtTokenProvider.getRefreshTokenValidityMs()).willReturn(REFRESH_TTL_MS);

            TokenResponse response = authService.login(loginRequest());

            assertThat(response.getAccessToken()).isEqualTo("access-token");
            assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
            assertThat(response.getTokenType()).isEqualTo("Bearer");

            then(valueOperations).should()
                .set("refresh:" + USER_ID, "refresh-token", Duration.ofMillis(REFRESH_TTL_MS));
        }

        @Test
        @DisplayName("하이픈이 포함된 전화번호도 숫자 형식으로 조회한다")
        void normalizesHyphenatedPhone() {
            LoginRequest request = loginRequest();
            request.setPhone("010-1234-5678");
            given(userMapper.findByPhone(PHONE)).willReturn(user());
            given(passwordEncoder.matches("raw-password", "encoded-password")).willReturn(true);
            given(jwtTokenProvider.createAccessToken(USER_ID, PHONE)).willReturn("access-token");
            given(jwtTokenProvider.createRefreshToken(USER_ID, PHONE)).willReturn("refresh-token");
            given(jwtTokenProvider.getRefreshTokenValidityMs()).willReturn(REFRESH_TTL_MS);

            authService.login(request);

            then(userMapper).should().findByPhone(PHONE);
        }

        @Test
        @DisplayName("존재하지 않는 전화번호면 401 예외를 던진다")
        void phoneNotFound() {
            given(userMapper.findByPhone(PHONE)).willReturn(null);

            assertThatThrownBy(() -> authService.login(loginRequest()))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
                    assertThat(exception.getMessage()).isEqualTo("전화번호 또는 비밀번호가 올바르지 않습니다.");
                });

            then(valueOperations).should(never()).set(anyString(), anyString(), (Duration) org.mockito.ArgumentMatchers.any());
        }

        @Test
        @DisplayName("비밀번호가 일치하지 않으면 401 예외를 던지며, 존재하지 않는 전화번호와 같은 메시지를 쓴다")
        void passwordMismatch() {
            given(userMapper.findByPhone(PHONE)).willReturn(user());
            given(passwordEncoder.matches("raw-password", "encoded-password")).willReturn(false);

            assertThatThrownBy(() -> authService.login(loginRequest()))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
                    assertThat(exception.getMessage()).isEqualTo("전화번호 또는 비밀번호가 올바르지 않습니다.");
                });
        }

        @Test
        @DisplayName("탈퇴한 사용자는 로그인할 수 없다")
        void withdrawnUser() {
            User user = user();
            user.setStatus(UserStatus.WITHDRAWN);
            given(userMapper.findByPhone(PHONE)).willReturn(user);

            assertThatThrownBy(() -> authService.login(loginRequest()))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                    assertThat(exception.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED));

            then(passwordEncoder).should(never()).matches(anyString(), anyString());
        }
    }

    @Nested
    @DisplayName("refresh")
    class Refresh {

        private RefreshTokenRequest request(String token) {
            RefreshTokenRequest request = new RefreshTokenRequest();
            request.setRefreshToken(token);
            return request;
        }

        @Test
        @DisplayName("정상 재발급이면 새 토큰을 발급하고 Redis 값을 갱신한다")
        void success() {
            given(jwtTokenProvider.validateToken("old-refresh")).willReturn(true);
            given(jwtTokenProvider.getUserId("old-refresh")).willReturn(USER_ID);
            given(valueOperations.get("refresh:" + USER_ID)).willReturn("old-refresh");
            given(userMapper.findById(USER_ID)).willReturn(user());
            given(jwtTokenProvider.createAccessToken(USER_ID, PHONE)).willReturn("new-access");
            given(jwtTokenProvider.createRefreshToken(USER_ID, PHONE)).willReturn("new-refresh");
            given(jwtTokenProvider.getRefreshTokenValidityMs()).willReturn(REFRESH_TTL_MS);

            TokenResponse response = authService.refresh(request("old-refresh"));

            assertThat(response.getAccessToken()).isEqualTo("new-access");
            assertThat(response.getRefreshToken()).isEqualTo("new-refresh");
            then(valueOperations).should()
                .set("refresh:" + USER_ID, "new-refresh", Duration.ofMillis(REFRESH_TTL_MS));
        }

        @Test
        @DisplayName("토큰이 유효하지 않으면 401 예외를 던지고 Redis를 조회하지 않는다")
        void invalidToken() {
            given(jwtTokenProvider.validateToken("bad-token")).willReturn(false);

            assertThatThrownBy(() -> authService.refresh(request("bad-token")))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
                    assertThat(exception.getMessage()).isEqualTo("리프레시 토큰이 유효하지 않습니다.");
                });

            then(valueOperations).should(never()).get(anyString());
        }

        @Test
        @DisplayName("Redis에 저장된 값과 다르면 401 예외를 던진다")
        void tokenMismatch() {
            given(jwtTokenProvider.validateToken("old-refresh")).willReturn(true);
            given(jwtTokenProvider.getUserId("old-refresh")).willReturn(USER_ID);
            given(valueOperations.get("refresh:" + USER_ID)).willReturn("different-refresh");

            assertThatThrownBy(() -> authService.refresh(request("old-refresh")))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
                    assertThat(exception.getMessage()).isEqualTo("리프레시 토큰이 일치하지 않습니다.");
                });
        }

        @Test
        @DisplayName("사용자를 찾을 수 없으면 401 예외를 던진다")
        void userNotFound() {
            given(jwtTokenProvider.validateToken("old-refresh")).willReturn(true);
            given(jwtTokenProvider.getUserId("old-refresh")).willReturn(USER_ID);
            given(valueOperations.get("refresh:" + USER_ID)).willReturn("old-refresh");
            given(userMapper.findById(USER_ID)).willReturn(null);

            assertThatThrownBy(() -> authService.refresh(request("old-refresh")))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
                    assertThat(exception.getMessage()).isEqualTo("사용자를 찾을 수 없습니다.");
                });
        }

        @Test
        @DisplayName("탈퇴한 사용자의 리프레시 토큰은 재발급하지 않는다")
        void withdrawnUser() {
            User user = user();
            user.setStatus(UserStatus.WITHDRAWN);
            given(jwtTokenProvider.validateToken("old-refresh")).willReturn(true);
            given(jwtTokenProvider.getUserId("old-refresh")).willReturn(USER_ID);
            given(valueOperations.get("refresh:" + USER_ID)).willReturn("old-refresh");
            given(userMapper.findById(USER_ID)).willReturn(user);

            assertThatThrownBy(() -> authService.refresh(request("old-refresh")))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                    assertThat(exception.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED));

            then(jwtTokenProvider).should(never()).createAccessToken(USER_ID, PHONE);
        }
    }
}
