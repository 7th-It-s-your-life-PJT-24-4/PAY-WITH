package com.paywith.guard.service;

import com.paywith.exception.BusinessException;
import com.paywith.guard.domain.GuardSeniorRelation;
import com.paywith.guard.dto.GuardPairingCodeResponse;
import com.paywith.guard.dto.WardPairingResponse;
import com.paywith.guard.mapper.GuardSeniorMapper;
import com.paywith.user.domain.Role;
import com.paywith.user.domain.User;
import com.paywith.user.mapper.UserMapper;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GuardServiceImpl implements GuardService {

    private static final String CODE_PATTERN = "\\d{5}";

    private static final Duration CODE_TTL = Duration.ofMinutes(5);
    private static final Duration ATTEMPTS_TTL = Duration.ofMinutes(5);
    private static final int MAX_ATTEMPTS = 5;

    private final GuardSeniorMapper guardSeniorMapper;
    private final UserMapper userMapper;
    private final RedisTemplate<String, String> redisTemplate;
    private final String inviteBaseUrl;
    private final SecureRandom random = new SecureRandom();

    public GuardServiceImpl(
        GuardSeniorMapper guardSeniorMapper,
        UserMapper userMapper,
        RedisTemplate<String, String> redisTemplate,
        @Value("${pairing.invite-base-url}") String inviteBaseUrl
    ) {
        this.guardSeniorMapper = guardSeniorMapper;
        this.userMapper = userMapper;
        this.redisTemplate = redisTemplate;
        this.inviteBaseUrl = inviteBaseUrl;
    }

    @Override
    public boolean verifyGuardOfWard(Long guardId, Long wardId) {
        return guardSeniorMapper.existsActiveRelation(guardId, wardId);
    }

    @Override
    public GuardPairingCodeResponse issuePairingCode(Long guardId) {
        requireRole(guardId, Role.GUARD);

        String codeByGuardKey = codeByGuardKey(guardId);
        String previousCode = redisTemplate.opsForValue().get(codeByGuardKey);
        if (previousCode != null) {
            redisTemplate.delete(codeKey(previousCode));
        }

        String code = generateCode();
        redisTemplate.opsForValue().set(codeKey(code), String.valueOf(guardId), CODE_TTL);
        redisTemplate.opsForValue().set(codeByGuardKey, code, CODE_TTL);

        LocalDateTime expiresAt = LocalDateTime.now().plus(CODE_TTL).truncatedTo(ChronoUnit.SECONDS);
        return new GuardPairingCodeResponse(code, inviteBaseUrl + "/" + code, expiresAt);
    }

    @Override
    @Transactional
    public WardPairingResponse pairWithCode(Long wardId, String pairingCode) {
        requireRole(wardId, Role.WARD);

        if (pairingCode == null || !pairingCode.matches(CODE_PATTERN)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "PAIRING_001", "인증 코드는 숫자 5자리여야 합니다.");
        }

        String attemptsKey = attemptsKey(wardId);
        String attemptsRaw = redisTemplate.opsForValue().get(attemptsKey);
        int attempts = attemptsRaw == null ? 0 : Integer.parseInt(attemptsRaw);
        if (attempts >= MAX_ATTEMPTS) {
            throw new BusinessException(
                HttpStatus.TOO_MANY_REQUESTS,
                "PAIRING_004",
                "인증 코드 입력 횟수를 초과했습니다. 잠시 후 다시 시도해주세요."

            );
        }

        String guardIdRaw = redisTemplate.opsForValue().get(codeKey(pairingCode));
        if (guardIdRaw == null) {
            redisTemplate.opsForValue().increment(attemptsKey);
            redisTemplate.expire(attemptsKey, ATTEMPTS_TTL);
            throw new BusinessException(
                HttpStatus.BAD_REQUEST,
                "PAIRING_002",
                "인증 코드가 유효하지 않거나 만료되었습니다."
            );
        }

        Long guardId = Long.valueOf(guardIdRaw);
        if (guardSeniorMapper.existsActiveRelation(guardId, wardId)) {
            throw new BusinessException(HttpStatus.CONFLICT, "PAIRING_003", "이미 연결된 보호자입니다.");
        }

        guardSeniorMapper.upsertActiveRelation(guardId, wardId);

        redisTemplate.delete(codeKey(pairingCode));
        redisTemplate.delete(codeByGuardKey(guardId));
        redisTemplate.delete(attemptsKey);

        GuardSeniorRelation relation = guardSeniorMapper.findRelation(guardId, wardId);
        String guardName = userMapper.findById(guardId).getName();

        return new WardPairingResponse(
            relation.getRelationId(),
            guardId,
            guardName,
            relation.getStatus(),
            relation.getConnectedAt()
        );
    }

    @Override
    @Transactional
    public void unpairWard(Long guardId, Long wardId) {
        requireRole(guardId, Role.GUARD);

        if (guardSeniorMapper.revokeActiveRelation(guardId, wardId) == 0) {
            throw new BusinessException(
                HttpStatus.NOT_FOUND,
                "PAIRING_005",
                "연결된 시니어를 찾을 수 없습니다."
            );
        }
    }

    private void requireRole(Long userId, Role expected) {
        User user = userMapper.findById(userId);
        if (user == null || user.getRole() != expected) {
            String message = expected == Role.GUARD
                ? "접근 권한이 없습니다."
                : "피보호자만 접근할 수 있습니다.";
            throw new BusinessException(HttpStatus.FORBIDDEN, "AUTH_004", message);
        }
    }

    private String generateCode() {
        return String.format("%05d", random.nextInt(100_000));
    }

    private String codeKey(String code) {
        return "pairing:code:" + code;
    }

    private String codeByGuardKey(Long guardId) {
        return "pairing:code-by-guard:" + guardId;
    }

    private String attemptsKey(Long wardId) {
        return "pairing:attempts:" + wardId;
    }
}
