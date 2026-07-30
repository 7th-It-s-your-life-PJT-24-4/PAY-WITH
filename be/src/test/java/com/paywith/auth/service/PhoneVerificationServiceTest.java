package com.paywith.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;

import com.paywith.auth.dto.PhoneCodeRequest;
import com.paywith.auth.dto.PhoneCodeResponse;
import com.paywith.auth.dto.PhoneVerifyRequest;
import com.paywith.auth.dto.PhoneVerifyResponse;
import com.paywith.auth.service.sms.SmsSender;
import com.paywith.exception.BusinessException;
import com.paywith.user.domain.User;
import com.paywith.user.mapper.UserMapper;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
@DisplayName("전화번호 인증 서비스")
class PhoneVerificationServiceTest {

    private static final String PHONE = "01012345678";
    private static final Duration CODE_TTL = Duration.ofSeconds(300);

    @Mock
    private UserMapper userMapper;
    @Mock
    private SmsSender smsSender;
    @Mock
    private RedisTemplate<String, String> redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;

    private PhoneVerificationService service;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        service = new PhoneVerificationService(userMapper, smsSender, redisTemplate);
    }

    private PhoneCodeRequest codeRequest(String phone, String purpose) {
        PhoneCodeRequest request = new PhoneCodeRequest();
        request.setPhone(phone);
        request.setPurpose(purpose);
        return request;
    }

    private PhoneVerifyRequest verifyRequest(String phone, String code) {
        PhoneVerifyRequest request = new PhoneVerifyRequest();
        request.setPhone(phone);
        request.setCode(code);
        return request;
    }

    @Nested
    @DisplayName("sendCode")
    class SendCode {

        @BeforeEach
        void setUp() {
            lenient().when(redisTemplate.getExpire(anyString())).thenReturn(null);
            lenient().when(valueOperations.increment(anyString())).thenReturn(1L);
        }

        @Test
        @DisplayName("정상 요청이면 코드를 생성하여 SMS로 전송하고 만료 시간을 반환한다")
        void sendCode_success() {
            given(userMapper.findByPhone(PHONE)).willReturn(null);

            PhoneCodeResponse response = service.sendCode(codeRequest(PHONE, "SIGNUP"));

            assertThat(response.getExpireIn()).isEqualTo((int) CODE_TTL.getSeconds());

            ArgumentCaptor<String> savedCode = ArgumentCaptor.forClass(String.class);
            then(valueOperations).should()
                .set(eq("phone:verify:code:" + PHONE), savedCode.capture(), eq(CODE_TTL));
            then(smsSender).should().send(eq(PHONE), eq(savedCode.getValue()));
            assertThat(savedCode.getValue()).matches("\\d{6}");

            then(redisTemplate).should().delete("phone:verify:attempts:" + PHONE);
            then(valueOperations).should()
                .set(eq("phone:verify:cooldown:" + PHONE), eq("1"), eq(Duration.ofSeconds(60)));
        }

        @Test
        @DisplayName("전화번호 형식이 올바르지 않으면 PHONE_001 예외를 던진다")
        void sendCode_invalidPhoneFormat() {
            assertThatThrownBy(() -> service.sendCode(codeRequest("abc", "SIGNUP")))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(exception.getCode()).isEqualTo("PHONE_001");
                });

            then(smsSender).should(never()).send(anyString(), anyString());
        }

        @Test
        @DisplayName("purpose가 SIGNUP/PASSWORD_RESET이 아니면 예외를 던진다")
        void sendCode_invalidPurpose() {
            assertThatThrownBy(() -> service.sendCode(codeRequest(PHONE, "ETC")))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                    assertThat(exception.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
        }

        @Test
        @DisplayName("SIGNUP인데 이미 가입된 번호면 USER_001 예외를 던진다")
        void sendCode_signupButAlreadyRegistered() {
            given(userMapper.findByPhone(PHONE)).willReturn(new User());

            assertThatThrownBy(() -> service.sendCode(codeRequest(PHONE, "SIGNUP")))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(exception.getCode()).isEqualTo("USER_001");
                });

            then(smsSender).should(never()).send(anyString(), anyString());
        }

        @Test
        @DisplayName("PASSWORD_RESET인데 미가입 번호면 USER_002 예외를 던진다")
        void sendCode_passwordResetButNotRegistered() {
            given(userMapper.findByPhone(PHONE)).willReturn(null);

            assertThatThrownBy(() -> service.sendCode(codeRequest(PHONE, "PASSWORD_RESET")))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(exception.getCode()).isEqualTo("USER_002");
                });

            then(smsSender).should(never()).send(anyString(), anyString());
        }

        @Test
        @DisplayName("재전송 쿨다운 중이면 PHONE_002 예외를 던지고 남은 시간을 함께 반환한다")
        void sendCode_resendCooldown() {
            given(userMapper.findByPhone(PHONE)).willReturn(null);
            given(redisTemplate.getExpire("phone:verify:cooldown:" + PHONE)).willReturn(30L);

            assertThatThrownBy(() -> service.sendCode(codeRequest(PHONE, "SIGNUP")))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
                    assertThat(exception.getCode()).isEqualTo("PHONE_002");
                    assertThat(exception.getData()).isEqualTo(java.util.Map.of("retryAfter", 30L));
                });

            then(smsSender).should(never()).send(anyString(), anyString());
        }

        @Test
        @DisplayName("일일 발송 한도(5회)를 초과하면 PHONE_003 예외를 던진다")
        void sendCode_dailyLimitExceeded() {
            given(userMapper.findByPhone(PHONE)).willReturn(null);
            given(valueOperations.increment("phone:verify:daily:" + PHONE)).willReturn(6L);

            assertThatThrownBy(() -> service.sendCode(codeRequest(PHONE, "SIGNUP")))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
                    assertThat(exception.getCode()).isEqualTo("PHONE_003");
                });

            then(smsSender).should(never()).send(anyString(), anyString());
        }
    }

    @Nested
    @DisplayName("verifyCode")
    class VerifyCode {

        @Test
        @DisplayName("형식이 올바르지 않으면 PHONE_001 예외를 던진다")
        void verifyCode_invalidFormat() {
            assertThatThrownBy(() -> service.verifyCode(verifyRequest(PHONE, "12a")))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(exception.getCode()).isEqualTo("PHONE_001");
                });
        }

        @Test
        @DisplayName("저장된 코드가 없으면(만료) AUTH_003 예외를 던진다")
        void verifyCode_expired() {
            given(valueOperations.get("phone:verify:attempts:" + PHONE)).willReturn(null);
            given(valueOperations.get("phone:verify:code:" + PHONE)).willReturn(null);

            assertThatThrownBy(() -> service.verifyCode(verifyRequest(PHONE, "123456")))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(exception.getCode()).isEqualTo("AUTH_003");
                    assertThat(exception.getMessage()).isEqualTo("인증 시간이 만료되었습니다.");
                });
        }

        @Test
        @DisplayName("코드가 일치하지 않으면 시도 횟수를 증가시키고 AUTH_003 예외를 던진다")
        void verifyCode_mismatch() {
            given(valueOperations.get("phone:verify:attempts:" + PHONE)).willReturn("1");
            given(valueOperations.get("phone:verify:code:" + PHONE)).willReturn("111111");

            assertThatThrownBy(() -> service.verifyCode(verifyRequest(PHONE, "222222")))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(exception.getCode()).isEqualTo("AUTH_003");
                    assertThat(exception.getMessage()).isEqualTo("인증번호가 올바르지 않습니다.");
                });

            then(valueOperations).should().increment("phone:verify:attempts:" + PHONE);
            then(redisTemplate).should().expire("phone:verify:attempts:" + PHONE, CODE_TTL);
        }

        @Test
        @DisplayName("최대 시도 횟수를 초과하면 코드를 폐기하고 PHONE_003 예외를 던진다")
        void verifyCode_maxAttemptsExceeded() {
            given(valueOperations.get("phone:verify:attempts:" + PHONE)).willReturn("5");

            assertThatThrownBy(() -> service.verifyCode(verifyRequest(PHONE, "123456")))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
                    assertThat(exception.getCode()).isEqualTo("PHONE_003");
                });

            then(redisTemplate).should().delete("phone:verify:code:" + PHONE);
            then(redisTemplate).should().delete("phone:verify:attempts:" + PHONE);
            then(valueOperations).should(never()).get("phone:verify:code:" + PHONE);
        }

        @Test
        @DisplayName("검증에 성공하면 토큰을 발급하고 기존 토큰을 무효화한다")
        void verifyCode_success() {
            given(valueOperations.get("phone:verify:attempts:" + PHONE)).willReturn(null);
            given(valueOperations.get("phone:verify:code:" + PHONE)).willReturn("123456");
            given(valueOperations.get("phone:verify:token-by-phone:" + PHONE)).willReturn("old-token");

            PhoneVerifyResponse response = service.verifyCode(verifyRequest(PHONE, "123456"));

            assertThat(response.getVerificationToken()).isNotBlank();

            then(redisTemplate).should().delete("phone:verify:code:" + PHONE);
            then(redisTemplate).should().delete("phone:verify:attempts:" + PHONE);
            then(redisTemplate).should().delete("phone:verify:token:old-token");
            then(valueOperations).should()
                .set(eq("phone:verify:token:" + response.getVerificationToken()), eq(PHONE), eq(Duration.ofMinutes(10)));
            then(valueOperations).should()
                .set(eq("phone:verify:token-by-phone:" + PHONE), eq(response.getVerificationToken()), eq(Duration.ofMinutes(10)));
        }

        @Test
        @DisplayName("검증에 성공했지만 기존 토큰이 없으면 토큰 삭제를 시도하지 않는다")
        void verifyCode_successWithoutPreviousToken() {
            given(valueOperations.get("phone:verify:attempts:" + PHONE)).willReturn(null);
            given(valueOperations.get("phone:verify:code:" + PHONE)).willReturn("123456");
            given(valueOperations.get("phone:verify:token-by-phone:" + PHONE)).willReturn(null);

            service.verifyCode(verifyRequest(PHONE, "123456"));

            ArgumentCaptor<String> deletedKeys = ArgumentCaptor.forClass(String.class);
            then(redisTemplate).should(org.mockito.Mockito.atLeastOnce()).delete(deletedKeys.capture());
            assertThat(deletedKeys.getAllValues())
                .noneMatch(key -> key.startsWith("phone:verify:token:"));
        }
    }
}