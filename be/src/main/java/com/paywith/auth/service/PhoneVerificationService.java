package com.paywith.auth.service;

import com.paywith.auth.dto.PhoneCodeRequest;
import com.paywith.auth.dto.PhoneCodeResponse;
import com.paywith.auth.dto.PhoneVerifyRequest;
import com.paywith.auth.dto.PhoneVerifyResponse;
import com.paywith.auth.service.sms.SmsSender;
import com.paywith.exception.BusinessException;
import com.paywith.user.mapper.UserMapper;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class PhoneVerificationService {

    private static final String PHONE_PATTERN = "01[016789]\\d{7,8}";
    private static final String CODE_PATTERN = "\\d{6}";

    private static final Duration CODE_TTL = Duration.ofSeconds(300);
    private static final Duration RESEND_COOLDOWN = Duration.ofSeconds(60);
    private static final Duration DAILY_LIMIT_TTL = Duration.ofHours(24);
    private static final Duration TOKEN_TTL = Duration.ofMinutes(10);
    private static final int DAILY_LIMIT = 5;
    private static final int MAX_ATTEMPTS = 5;

    private final UserMapper userMapper;
    private final SmsSender smsSender;
    private final RedisTemplate<String, String> redisTemplate;
    private final SecureRandom random = new SecureRandom();

    public PhoneVerificationService(
        UserMapper userMapper,
        SmsSender smsSender,
        RedisTemplate<String, String> redisTemplate
    ) {
        this.userMapper = userMapper;
        this.smsSender = smsSender;
        this.redisTemplate = redisTemplate;
    }

    public PhoneCodeResponse sendCode(PhoneCodeRequest request) {
        String phone = request.getPhone();
        if (phone == null || !phone.matches(PHONE_PATTERN)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "PHONE_001", "전화번호 형식이 올바르지 않습니다.");
        }
        Purpose purpose = parsePurpose(request.getPurpose());

        boolean userExists = userMapper.findByPhone(phone) != null;
        if (purpose == Purpose.SIGNUP && userExists) {
            throw new BusinessException(HttpStatus.CONFLICT, "USER_001", "이미 가입된 전화번호입니다.");
        }
        if (purpose == Purpose.PASSWORD_RESET && !userExists) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "USER_002", "가입되지 않은 전화번호입니다.");
        }

        String cooldownKey = cooldownKey(phone);
        Long retryAfter = redisTemplate.getExpire(cooldownKey);
        if (retryAfter != null && retryAfter > 0) {
            throw new BusinessException(
                HttpStatus.TOO_MANY_REQUESTS,
                "PHONE_002",
                "요청이 너무 잦습니다. 잠시 후 다시 시도해주세요.",
                Map.of("retryAfter", retryAfter)
            );
        }

        String dailyCountKey = dailyCountKey(phone);
        Long dailyCount = redisTemplate.opsForValue().increment(dailyCountKey);
        if (dailyCount != null && dailyCount == 1L) {
            redisTemplate.expire(dailyCountKey, DAILY_LIMIT_TTL);
        }
        if (dailyCount != null && dailyCount > DAILY_LIMIT) {
            throw new BusinessException(
                HttpStatus.TOO_MANY_REQUESTS,
                "PHONE_003",
                "일일 인증번호 발송 한도(5회)를 초과했습니다. 내일 다시 시도해주세요."
            );
        }

        String code = generateCode();
        redisTemplate.opsForValue().set(codeKey(phone), code, CODE_TTL);
        redisTemplate.delete(attemptsKey(phone));
        redisTemplate.opsForValue().set(cooldownKey, "1", RESEND_COOLDOWN);

        smsSender.send(phone, code);

        return new PhoneCodeResponse((int) CODE_TTL.getSeconds());
    }

    public PhoneVerifyResponse verifyCode(PhoneVerifyRequest request) {
        String phone = request.getPhone();
        String code = request.getCode();
        if (phone == null || !phone.matches(PHONE_PATTERN) || code == null || !code.matches(CODE_PATTERN)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "PHONE_001", "잘못된 형식으로 입력하였습니다.");
        }

        String attemptsKey = attemptsKey(phone);
        String attemptsRaw = redisTemplate.opsForValue().get(attemptsKey);
        int attempts = attemptsRaw == null ? 0 : Integer.parseInt(attemptsRaw);
        if (attempts >= MAX_ATTEMPTS) {
            redisTemplate.delete(codeKey(phone));
            redisTemplate.delete(attemptsKey);
            throw new BusinessException(
                HttpStatus.TOO_MANY_REQUESTS,
                "PHONE_003",
                "시도 횟수를 초과했습니다. 인증번호를 다시 받아주세요."
            );
        }

        String savedCode = redisTemplate.opsForValue().get(codeKey(phone));
        if (savedCode == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "AUTH_003", "인증 시간이 만료되었습니다.");
        }
        if (!savedCode.equals(code)) {
            redisTemplate.opsForValue().increment(attemptsKey);
            redisTemplate.expire(attemptsKey, CODE_TTL);
            throw new BusinessException(HttpStatus.BAD_REQUEST, "AUTH_003", "인증번호가 올바르지 않습니다.");
        }

        redisTemplate.delete(codeKey(phone));
        redisTemplate.delete(attemptsKey);

        String tokenByPhoneKey = tokenByPhoneKey(phone);
        String previousToken = redisTemplate.opsForValue().get(tokenByPhoneKey);
        if (previousToken != null) {
            redisTemplate.delete(tokenKey(previousToken));
        }

        String token = UUID.randomUUID().toString().replace("-", "");
        redisTemplate.opsForValue().set(tokenKey(token), phone, TOKEN_TTL);
        redisTemplate.opsForValue().set(tokenByPhoneKey, token, TOKEN_TTL);

        return new PhoneVerifyResponse(token);
    }

    private Purpose parsePurpose(String purpose) {
        try {
            return Purpose.valueOf(purpose);
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "purpose는 SIGNUP 또는 PASSWORD_RESET이어야 합니다.");
        }
    }

    private String generateCode() {
        return String.format("%06d", random.nextInt(1_000_000));
    }

    private String codeKey(String phone) {
        return "phone:verify:code:" + phone;
    }

    private String attemptsKey(String phone) {
        return "phone:verify:attempts:" + phone;
    }

    private String cooldownKey(String phone) {
        return "phone:verify:cooldown:" + phone;
    }

    private String dailyCountKey(String phone) {
        return "phone:verify:daily:" + phone;
    }

    private String tokenKey(String token) {
        return "phone:verify:token:" + token;
    }

    private String tokenByPhoneKey(String phone) {
        return "phone:verify:token-by-phone:" + phone;
    }

    private enum Purpose {
        SIGNUP,
        PASSWORD_RESET
    }
}