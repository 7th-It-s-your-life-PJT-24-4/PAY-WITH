package com.paywith.guard.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;

import com.paywith.exception.BusinessException;
import com.paywith.guard.domain.GuardSeniorRelation;
import com.paywith.guard.dto.GuardPairingCodeResponse;
import com.paywith.guard.dto.WardPairingResponse;
import com.paywith.guard.mapper.GuardSeniorMapper;
import com.paywith.user.domain.Role;
import com.paywith.user.domain.User;
import com.paywith.user.mapper.UserMapper;
import java.time.Duration;
import java.time.LocalDateTime;
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
@DisplayName("보호자-시니어 페어링 서비스")
class GuardServiceImplTest {

    private static final Long GUARD_ID = 1L;
    private static final Long WARD_ID = 2L;
    private static final String BASE_URL = "https://paywith.link";
    private static final Duration CODE_TTL = Duration.ofMinutes(5);

    @Mock
    private GuardSeniorMapper guardSeniorMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private RedisTemplate<String, String> redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;

    private GuardServiceImpl service;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        service = new GuardServiceImpl(guardSeniorMapper, userMapper, redisTemplate, BASE_URL);
    }

    private User user(Role role) {
        User user = new User();
        user.setRole(role);
        return user;
    }

    @Nested
    @DisplayName("issuePairingCode")
    class IssuePairingCode {

        @Test
        @DisplayName("GUARD가 요청하면 5자리 코드와 링크를 발급한다")
        void success() {
            given(userMapper.findById(GUARD_ID)).willReturn(user(Role.GUARD));
            given(valueOperations.get("pairing:code-by-guard:" + GUARD_ID)).willReturn(null);

            GuardPairingCodeResponse response = service.issuePairingCode(GUARD_ID);

            assertThat(response.getCode()).matches("\\d{5}");
            assertThat(response.getInviteUrl()).isEqualTo(BASE_URL + "/" + response.getCode());
            assertThat(response.getExpiresAt())
                .isAfter(LocalDateTime.now().plus(CODE_TTL).minusSeconds(5))
                .isBefore(LocalDateTime.now().plus(CODE_TTL).plusSeconds(5));

            then(valueOperations).should()
                .set(eq("pairing:code:" + response.getCode()), eq(String.valueOf(GUARD_ID)), eq(CODE_TTL));
            then(valueOperations).should()
                .set(eq("pairing:code-by-guard:" + GUARD_ID), eq(response.getCode()), eq(CODE_TTL));
        }

        @Test
        @DisplayName("WARD가 요청하면 AUTH_004 예외를 던진다")
        void wardRequester_forbidden() {
            given(userMapper.findById(GUARD_ID)).willReturn(user(Role.WARD));

            assertThatThrownBy(() -> service.issuePairingCode(GUARD_ID))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(exception.getCode()).isEqualTo("AUTH_004");
                    assertThat(exception.getMessage()).isEqualTo("접근 권한이 없습니다.");
                });

            then(valueOperations).should(never()).set(anyString(), anyString(), eq(CODE_TTL));
        }

        @Test
        @DisplayName("존재하지 않는 사용자면 AUTH_004 예외를 던진다")
        void userNotFound_forbidden() {
            given(userMapper.findById(GUARD_ID)).willReturn(null);

            assertThatThrownBy(() -> service.issuePairingCode(GUARD_ID))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                    assertThat(exception.getCode()).isEqualTo("AUTH_004"));
        }

        @Test
        @DisplayName("재발급하면 이전 코드를 무효화한다")
        void reissue_invalidatesPreviousCode() {
            given(userMapper.findById(GUARD_ID)).willReturn(user(Role.GUARD));
            given(valueOperations.get("pairing:code-by-guard:" + GUARD_ID)).willReturn("11111");

            service.issuePairingCode(GUARD_ID);

            then(redisTemplate).should().delete("pairing:code:11111");
        }
    }

    @Nested
    @DisplayName("pairWithCode")
    class PairWithCode {

        @BeforeEach
        void setUp() {
            lenient().when(userMapper.findById(WARD_ID)).thenReturn(user(Role.WARD));
        }

        @Test
        @DisplayName("WARD가 아니면 AUTH_004 예외를 던진다")
        void notWard_forbidden() {
            given(userMapper.findById(WARD_ID)).willReturn(user(Role.GUARD));

            assertThatThrownBy(() -> service.pairWithCode(WARD_ID, "12345"))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(exception.getCode()).isEqualTo("AUTH_004");
                    assertThat(exception.getMessage()).isEqualTo("피보호자만 접근할 수 있습니다.");
                });
        }

        @Test
        @DisplayName("코드 형식이 숫자 5자리가 아니면 PAIRING_001, 시도 횟수는 늘지 않는다")
        void invalidFormat() {
            for (String invalid : new String[] {null, "", "1234", "123456", "12a45"}) {
                assertThatThrownBy(() -> service.pairWithCode(WARD_ID, invalid))
                    .isInstanceOfSatisfying(BusinessException.class, exception -> {
                        assertThat(exception.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                        assertThat(exception.getCode()).isEqualTo("PAIRING_001");
                    });
            }

            then(valueOperations).should(never()).increment(anyString());
        }

        @Test
        @DisplayName("시도 횟수가 5회 이상이면 PAIRING_004 예외를 던진다")
        void attemptsExceeded() {
            given(valueOperations.get("pairing:attempts:" + WARD_ID)).willReturn("5");

            assertThatThrownBy(() -> service.pairWithCode(WARD_ID, "12345"))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
                    assertThat(exception.getCode()).isEqualTo("PAIRING_004");
                });

            then(valueOperations).should(never()).get("pairing:code:12345");
        }

        @Test
        @DisplayName("코드가 Redis에 없으면 PAIRING_002 예외를 던지고 시도 횟수를 늘린다")
        void codeNotFound() {
            given(valueOperations.get("pairing:attempts:" + WARD_ID)).willReturn(null);
            given(valueOperations.get("pairing:code:12345")).willReturn(null);

            assertThatThrownBy(() -> service.pairWithCode(WARD_ID, "12345"))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(exception.getCode()).isEqualTo("PAIRING_002");
                });

            then(valueOperations).should().increment("pairing:attempts:" + WARD_ID);
            then(redisTemplate).should().expire(eq("pairing:attempts:" + WARD_ID), eq(Duration.ofMinutes(5)));
        }

        @Test
        @DisplayName("이미 그 보호자와 ACTIVE 관계면 PAIRING_003 예외를 던지고 코드를 소비하지 않는다")
        void alreadyPaired() {
            given(valueOperations.get("pairing:attempts:" + WARD_ID)).willReturn(null);
            given(valueOperations.get("pairing:code:12345")).willReturn(String.valueOf(GUARD_ID));
            given(guardSeniorMapper.existsActiveRelation(GUARD_ID, WARD_ID)).willReturn(true);

            assertThatThrownBy(() -> service.pairWithCode(WARD_ID, "12345"))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(exception.getCode()).isEqualTo("PAIRING_003");
                    assertThat(exception.getMessage()).isEqualTo("이미 연결된 보호자입니다.");
                });

            then(guardSeniorMapper).should(never()).upsertActiveRelation(GUARD_ID, WARD_ID);
            then(redisTemplate).should(never()).delete("pairing:code:12345");
        }

        @Test
        @DisplayName("정상 코드면 연동을 생성하고 코드·시도횟수를 초기화한다")
        void success() {
            given(valueOperations.get("pairing:attempts:" + WARD_ID)).willReturn("2");
            given(valueOperations.get("pairing:code:12345")).willReturn(String.valueOf(GUARD_ID));
            given(guardSeniorMapper.existsActiveRelation(GUARD_ID, WARD_ID)).willReturn(false);

            GuardSeniorRelation relation = new GuardSeniorRelation();
            relation.setRelationId(10L);
            relation.setGuardId(GUARD_ID);
            relation.setWardId(WARD_ID);
            relation.setStatus("ACTIVE");
            relation.setConnectedAt(LocalDateTime.of(2026, 7, 31, 15, 0));
            given(guardSeniorMapper.findRelation(GUARD_ID, WARD_ID)).willReturn(relation);

            User guard = user(Role.GUARD);
            guard.setName("김보호");
            given(userMapper.findById(GUARD_ID)).willReturn(guard);

            WardPairingResponse response = service.pairWithCode(WARD_ID, "12345");

            assertThat(response.getRelationId()).isEqualTo(10L);
            assertThat(response.getGuardId()).isEqualTo(GUARD_ID);
            assertThat(response.getGuardName()).isEqualTo("김보호");
            assertThat(response.getStatus()).isEqualTo("ACTIVE");
            assertThat(response.getConnectedAt()).isEqualTo(relation.getConnectedAt());

            then(guardSeniorMapper).should().upsertActiveRelation(GUARD_ID, WARD_ID);
            then(redisTemplate).should().delete("pairing:code:12345");
            then(redisTemplate).should().delete("pairing:code-by-guard:" + GUARD_ID);
            then(redisTemplate).should().delete("pairing:attempts:" + WARD_ID);
        }
    }

    @Nested
    @DisplayName("unpairWard")
    class UnpairWard {

        @Test
        @DisplayName("보호자가 ACTIVE 관계를 해제한다")
        void success() {
            given(userMapper.findById(GUARD_ID)).willReturn(user(Role.GUARD));
            given(guardSeniorMapper.revokeActiveRelation(GUARD_ID, WARD_ID)).willReturn(1);

            service.unpairWard(GUARD_ID, WARD_ID);

            then(guardSeniorMapper).should().revokeActiveRelation(GUARD_ID, WARD_ID);
        }

        @Test
        @DisplayName("ACTIVE 관계가 없으면 PAIRING_005 예외를 던진다")
        void relationNotFound() {
            given(userMapper.findById(GUARD_ID)).willReturn(user(Role.GUARD));
            given(guardSeniorMapper.revokeActiveRelation(GUARD_ID, WARD_ID)).willReturn(0);

            assertThatThrownBy(() -> service.unpairWard(GUARD_ID, WARD_ID))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(exception.getCode()).isEqualTo("PAIRING_005");
                });
        }
    }
}
