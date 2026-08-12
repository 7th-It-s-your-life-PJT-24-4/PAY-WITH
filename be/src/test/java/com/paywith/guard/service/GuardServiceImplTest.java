package com.paywith.guard.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;

import com.paywith.exception.BusinessException;
import com.paywith.guard.domain.GuardSeniorRelation;
import com.paywith.guard.dto.GuardPairingCodeResponse;
import com.paywith.guard.dto.PairingRequestResponse;
import com.paywith.guard.dto.PairingStatusResponse;
import com.paywith.guard.dto.PendingPairingRequestResponse;
import com.paywith.guard.dto.WardPairingResponse;
import com.paywith.guard.mapper.GuardSeniorMapper;
import com.paywith.user.domain.Role;
import com.paywith.user.domain.User;
import com.paywith.user.mapper.UserMapper;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;
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
    private static final Long OTHER_GUARD_ID = 3L;
    private static final Long OTHER_WARD_ID = 4L;
    private static final String BASE_URL = "https://paywith.link";
    private static final Duration CODE_TTL = Duration.ofMinutes(5);
    private static final Duration REQUEST_TTL = Duration.ofMinutes(2);
    private static final String REQUEST_ID = "3f2504e0-4f89-41d3-9a0c-0305e82c3301";

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

    @Nested
    @DisplayName("createPairingRequest")
    class CreatePairingRequest {

        @BeforeEach
        void setUp() {
            lenient().when(userMapper.findById(WARD_ID)).thenReturn(user(Role.WARD));
        }

        @Test
        @DisplayName("WARD가 아니면 AUTH_004 예외를 던진다")
        void notWard_forbidden() {
            given(userMapper.findById(WARD_ID)).willReturn(user(Role.GUARD));

            assertThatThrownBy(() -> service.createPairingRequest(WARD_ID, "12345"))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(exception.getCode()).isEqualTo("AUTH_004");
                });
        }

        @Test
        @DisplayName("코드 형식이 숫자 5자리가 아니면 PAIRING_001, 시도 횟수는 늘지 않는다")
        void invalidFormat() {
            for (String invalid : new String[] {null, "", "1234", "123456", "12a45"}) {
                assertThatThrownBy(() -> service.createPairingRequest(WARD_ID, invalid))
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

            assertThatThrownBy(() -> service.createPairingRequest(WARD_ID, "12345"))
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

            assertThatThrownBy(() -> service.createPairingRequest(WARD_ID, "12345"))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(exception.getCode()).isEqualTo("PAIRING_002");
                });

            then(valueOperations).should().increment("pairing:attempts:" + WARD_ID);
            then(redisTemplate).should().expire(eq("pairing:attempts:" + WARD_ID), eq(Duration.ofMinutes(5)));
        }

        @Test
        @DisplayName("이미 그 보호자와 ACTIVE 관계면 PAIRING_003 예외를 던지고 요청을 만들지 않는다")
        void alreadyPaired() {
            given(valueOperations.get("pairing:attempts:" + WARD_ID)).willReturn(null);
            given(valueOperations.get("pairing:code:12345")).willReturn(String.valueOf(GUARD_ID));
            given(guardSeniorMapper.existsActiveRelation(GUARD_ID, WARD_ID)).willReturn(true);

            assertThatThrownBy(() -> service.createPairingRequest(WARD_ID, "12345"))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(exception.getCode()).isEqualTo("PAIRING_003");
                });

            then(redisTemplate).should(never()).delete("pairing:code:12345");
            then(valueOperations).should(never())
                .set(startsWith("pairing:request:"), anyString(), any(Duration.class));
        }

        @Test
        @DisplayName("정상 코드면 PENDING 요청을 만들고 코드·시도횟수를 소비한다")
        void success() {
            given(valueOperations.get("pairing:attempts:" + WARD_ID)).willReturn("2");
            given(valueOperations.get("pairing:code:12345")).willReturn(String.valueOf(GUARD_ID));
            given(guardSeniorMapper.existsActiveRelation(GUARD_ID, WARD_ID)).willReturn(false);

            PairingRequestResponse response = service.createPairingRequest(WARD_ID, "12345");

            String requestId = response.getRequestId();
            assertThat(requestId).isNotBlank();
            assertThatCode(() -> UUID.fromString(requestId)).doesNotThrowAnyException();

            then(valueOperations).should().set(
                "pairing:request:" + requestId,
                WARD_ID + ":" + GUARD_ID + ":PENDING",
                REQUEST_TTL);
            then(valueOperations).should()
                .set("pairing:request-by-guard:" + GUARD_ID, requestId, REQUEST_TTL);

            then(redisTemplate).should().delete("pairing:code:12345");
            then(redisTemplate).should().delete("pairing:code-by-guard:" + GUARD_ID);
            then(redisTemplate).should().delete("pairing:attempts:" + WARD_ID);

            // 승인 전이므로 이 시점에는 아직 연동되면 안 된다
            then(guardSeniorMapper).should(never()).upsertActiveRelation(GUARD_ID, WARD_ID);
        }
    }

    @Nested
    @DisplayName("checkPairingStatus")
    class CheckPairingStatus {

        @Test
        @DisplayName("요청이 없거나 TTL로 사라졌으면 EXPIRED를 반환한다")
        void missingRequest_expired() {
            given(valueOperations.get("pairing:request:" + REQUEST_ID)).willReturn(null);

            PairingStatusResponse response = service.checkPairingStatus(WARD_ID, REQUEST_ID);

            assertThat(response.getStatus()).isEqualTo("EXPIRED");
        }

        @Test
        @DisplayName("아직 승인 전이면 PENDING을 반환한다")
        void pending() {
            given(valueOperations.get("pairing:request:" + REQUEST_ID))
                .willReturn(WARD_ID + ":" + GUARD_ID + ":PENDING");

            assertThat(service.checkPairingStatus(WARD_ID, REQUEST_ID).getStatus()).isEqualTo("PENDING");
        }

        @Test
        @DisplayName("보호자가 승인했으면 CONFIRMED를 반환한다")
        void confirmed() {
            given(valueOperations.get("pairing:request:" + REQUEST_ID))
                .willReturn(WARD_ID + ":" + GUARD_ID + ":CONFIRMED");

            assertThat(service.checkPairingStatus(WARD_ID, REQUEST_ID).getStatus()).isEqualTo("CONFIRMED");
        }

        @Test
        @DisplayName("남의 요청을 조회하면 PAIRING_006 예외를 던진다")
        void otherWardsRequest_forbidden() {
            given(valueOperations.get("pairing:request:" + REQUEST_ID))
                .willReturn(OTHER_WARD_ID + ":" + GUARD_ID + ":PENDING");

            assertThatThrownBy(() -> service.checkPairingStatus(WARD_ID, REQUEST_ID))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(exception.getCode()).isEqualTo("PAIRING_006");
                });
        }
    }

    @Nested
    @DisplayName("findPendingRequest")
    class FindPendingRequest {

        @BeforeEach
        void setUp() {
            lenient().when(userMapper.findById(GUARD_ID)).thenReturn(user(Role.GUARD));
        }

        @Test
        @DisplayName("GUARD가 아니면 AUTH_004 예외를 던진다")
        void notGuard_forbidden() {
            given(userMapper.findById(GUARD_ID)).willReturn(user(Role.WARD));

            assertThatThrownBy(() -> service.findPendingRequest(GUARD_ID))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(exception.getCode()).isEqualTo("AUTH_004");
                });
        }

        @Test
        @DisplayName("대기 중인 요청이 없으면 null을 반환한다")
        void noRequestId_null() {
            given(valueOperations.get("pairing:request-by-guard:" + GUARD_ID)).willReturn(null);

            assertThat(service.findPendingRequest(GUARD_ID)).isNull();
        }

        @Test
        @DisplayName("요청 본문이 TTL로 사라졌으면 null을 반환한다")
        void requestExpired_null() {
            given(valueOperations.get("pairing:request-by-guard:" + GUARD_ID)).willReturn(REQUEST_ID);
            given(valueOperations.get("pairing:request:" + REQUEST_ID)).willReturn(null);

            assertThat(service.findPendingRequest(GUARD_ID)).isNull();
        }

        @Test
        @DisplayName("이미 처리된 요청이면 null을 반환한다")
        void notPending_null() {
            given(valueOperations.get("pairing:request-by-guard:" + GUARD_ID)).willReturn(REQUEST_ID);
            given(valueOperations.get("pairing:request:" + REQUEST_ID))
                .willReturn(WARD_ID + ":" + GUARD_ID + ":CONFIRMED");

            assertThat(service.findPendingRequest(GUARD_ID)).isNull();
        }

        @Test
        @DisplayName("PENDING 요청이면 피보호자 이름과 마스킹된 전화번호를 반환한다")
        void success() {
            given(valueOperations.get("pairing:request-by-guard:" + GUARD_ID)).willReturn(REQUEST_ID);
            given(valueOperations.get("pairing:request:" + REQUEST_ID))
                .willReturn(WARD_ID + ":" + GUARD_ID + ":PENDING");

            User ward = user(Role.WARD);
            ward.setName("박시니");
            ward.setPhone("01098765432");
            given(userMapper.findById(WARD_ID)).willReturn(ward);

            PendingPairingRequestResponse response = service.findPendingRequest(GUARD_ID);

            assertThat(response.getRequestId()).isEqualTo(REQUEST_ID);
            assertThat(response.getWardName()).isEqualTo("박시니");
            assertThat(response.getWardPhoneMasked()).isEqualTo("010****5432");
        }
    }

    @Nested
    @DisplayName("confirmPairingRequest")
    class ConfirmPairingRequest {

        @BeforeEach
        void setUp() {
            lenient().when(userMapper.findById(GUARD_ID)).thenReturn(user(Role.GUARD));
        }

        @Test
        @DisplayName("GUARD가 아니면 AUTH_004 예외를 던진다")
        void notGuard_forbidden() {
            given(userMapper.findById(GUARD_ID)).willReturn(user(Role.WARD));

            assertThatThrownBy(() -> service.confirmPairingRequest(GUARD_ID, REQUEST_ID))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(exception.getCode()).isEqualTo("AUTH_004");
                });

            then(guardSeniorMapper).should(never()).upsertActiveRelation(GUARD_ID, WARD_ID);
        }

        @Test
        @DisplayName("요청이 만료됐거나 없으면 PAIRING_007 예외를 던진다")
        void expiredRequest() {
            given(valueOperations.get("pairing:request:" + REQUEST_ID)).willReturn(null);

            assertThatThrownBy(() -> service.confirmPairingRequest(GUARD_ID, REQUEST_ID))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(exception.getCode()).isEqualTo("PAIRING_007");
                });

            then(guardSeniorMapper).should(never()).upsertActiveRelation(GUARD_ID, WARD_ID);
        }

        @Test
        @DisplayName("자기에게 온 요청이 아니면 PAIRING_008 예외를 던진다")
        void otherGuardsRequest_forbidden() {
            given(valueOperations.get("pairing:request:" + REQUEST_ID))
                .willReturn(WARD_ID + ":" + OTHER_GUARD_ID + ":PENDING");

            assertThatThrownBy(() -> service.confirmPairingRequest(GUARD_ID, REQUEST_ID))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(exception.getCode()).isEqualTo("PAIRING_008");
                });

            then(guardSeniorMapper).should(never()).upsertActiveRelation(GUARD_ID, WARD_ID);
        }

        @Test
        @DisplayName("이미 확정된 요청이면 PAIRING_009 예외를 던져 중복 승인을 막는다")
        void alreadyConfirmed_conflict() {
            given(valueOperations.get("pairing:request:" + REQUEST_ID))
                .willReturn(WARD_ID + ":" + GUARD_ID + ":CONFIRMED");

            assertThatThrownBy(() -> service.confirmPairingRequest(GUARD_ID, REQUEST_ID))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(exception.getCode()).isEqualTo("PAIRING_009");
                });

            then(guardSeniorMapper).should(never()).upsertActiveRelation(GUARD_ID, WARD_ID);
        }

        @Test
        @DisplayName("PENDING 요청을 승인하면 연동을 생성하고 상태를 CONFIRMED로 바꾼다")
        void success() {
            given(valueOperations.get("pairing:request:" + REQUEST_ID))
                .willReturn(WARD_ID + ":" + GUARD_ID + ":PENDING");

            GuardSeniorRelation relation = new GuardSeniorRelation();
            relation.setRelationId(10L);
            relation.setGuardId(GUARD_ID);
            relation.setWardId(WARD_ID);
            relation.setStatus("ACTIVE");
            relation.setConnectedAt(LocalDateTime.of(2026, 8, 12, 10, 0));
            given(guardSeniorMapper.findRelation(GUARD_ID, WARD_ID)).willReturn(relation);

            User guard = user(Role.GUARD);
            guard.setName("김보호");
            given(userMapper.findById(GUARD_ID)).willReturn(guard);

            WardPairingResponse response = service.confirmPairingRequest(GUARD_ID, REQUEST_ID);

            assertThat(response.getRelationId()).isEqualTo(10L);
            assertThat(response.getGuardId()).isEqualTo(GUARD_ID);
            assertThat(response.getGuardName()).isEqualTo("김보호");
            assertThat(response.getStatus()).isEqualTo("ACTIVE");
            assertThat(response.getConnectedAt()).isEqualTo(relation.getConnectedAt());

            then(guardSeniorMapper).should().upsertActiveRelation(GUARD_ID, WARD_ID);
            // 피보호자 폴링이 결과를 한 번은 받아갈 수 있도록 짧게 남겨둔다
            then(valueOperations).should().set(
                "pairing:request:" + REQUEST_ID,
                WARD_ID + ":" + GUARD_ID + ":CONFIRMED",
                Duration.ofSeconds(30));
            then(redisTemplate).should().delete("pairing:request-by-guard:" + GUARD_ID);
        }
    }
}
