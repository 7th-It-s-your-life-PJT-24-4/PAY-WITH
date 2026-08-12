package com.paywith.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;

import com.paywith.auth.service.PhoneVerificationService;
import com.paywith.exception.BusinessException;
import com.paywith.user.domain.Role;
import com.paywith.user.domain.User;
import com.paywith.user.domain.UserStatus;
import com.paywith.user.dto.UserCreateRequest;
import com.paywith.user.dto.UserResponse;
import com.paywith.user.dto.UserUpdateRequest;
import com.paywith.user.mapper.UserMapper;
import com.paywith.wallet.mapper.WalletMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
@DisplayName("사용자 서비스")
class UserServiceTest {

    private static final String PHONE = "01012345678";
    private static final String TOKEN = "verification-token";

    @Mock
    private UserMapper userMapper;
    @Mock
    private WalletMapper walletMapper;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private PhoneVerificationService phoneVerificationService;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userMapper, walletMapper, passwordEncoder, phoneVerificationService);
    }

    private UserCreateRequest createRequest(String role) {
        UserCreateRequest request = new UserCreateRequest();
        request.setRole(role);
        request.setPhone(PHONE);
        request.setPassword("password123");
        request.setName("홍길동");
        request.setBirthDate("19900101");
        request.setGender("남");
        request.setPaymentPassword("123456");
        request.setVerificationToken(TOKEN);
        return request;
    }

    @Nested
    @DisplayName("create")
    class Create {

        @BeforeEach
        void setUp() {
            lenient().when(passwordEncoder.encode(anyString())).thenReturn("encoded");
            lenient().doAnswer(invocation -> {
                User user = invocation.getArgument(0);
                user.setId(1L);
                return 1;
            }).when(userMapper).insert(any(User.class));
        }

        @Test
        @DisplayName("WARD로 가입하면 지갑이 함께 생성되고 토큰을 검증·소비한다")
        void success_ward() {
            given(userMapper.findByPhone(PHONE)).willReturn(null);
            given(userMapper.findById(1L)).willReturn(wardUser());

            UserResponse response = userService.create(createRequest("WARD"));

            assertThat(response.getPhone()).isEqualTo(PHONE);

            then(phoneVerificationService).should().requireValidToken(TOKEN, PHONE);
            then(userMapper).should().insert(any(User.class));
            then(walletMapper).should().insert(any());
            then(phoneVerificationService).should().invalidateToken(TOKEN, PHONE);
        }

        @Test
        @DisplayName("GUARD로 가입하면 지갑이 생성되지 않는다")
        void success_guard() {
            given(userMapper.findByPhone(PHONE)).willReturn(null);
            given(userMapper.findById(1L)).willReturn(guardUser());

            userService.create(createRequest("GUARD"));

            then(walletMapper).should(never()).insert(any());
        }

        @Test
        @DisplayName("하이픈이 포함된 전화번호도 숫자 형식으로 인증·저장한다")
        void normalizesHyphenatedPhone() {
            UserCreateRequest request = createRequest("WARD");
            request.setPhone("010-1234-5678");
            given(userMapper.findByPhone(PHONE)).willReturn(null);
            given(userMapper.findById(1L)).willReturn(wardUser());

            userService.create(request);

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            then(phoneVerificationService).should().requireValidToken(TOKEN, PHONE);
            then(userMapper).should().insert(captor.capture());
            then(phoneVerificationService).should().invalidateToken(TOKEN, PHONE);
            assertThat(captor.getValue().getPhone()).isEqualTo(PHONE);
        }

        @Test
        @DisplayName("verificationToken이 유효하지 않으면 가입 로직을 실행하지 않는다")
        void invalidToken_stopsBeforeCreate() {
            org.mockito.Mockito.doThrow(new BusinessException(HttpStatus.BAD_REQUEST, "PHONE_004", "휴대폰 인증이 필요합니다."))
                .when(phoneVerificationService).requireValidToken(TOKEN, PHONE);

            assertThatThrownBy(() -> userService.create(createRequest("WARD")))
                .isInstanceOf(BusinessException.class);

            then(userMapper).should(never()).findByPhone(anyString());
            then(userMapper).should(never()).insert(any(User.class));
            then(walletMapper).should(never()).insert(any());
            then(phoneVerificationService).should(never()).invalidateToken(anyString(), anyString());
        }

        @Test
        @DisplayName("이미 가입된 전화번호면 CONFLICT 예외를 던지고 insert하지 않는다")
        void duplicatePhone() {
            given(userMapper.findByPhone(PHONE)).willReturn(wardUser());

            assertThatThrownBy(() -> userService.create(createRequest("WARD")))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(exception.getMessage()).isEqualTo("이미 사용 중인 전화번호입니다.");
                });

            then(userMapper).should(never()).insert(any(User.class));
        }

        @Test
        @DisplayName("role이 WARD/GUARD가 아니면 BAD_REQUEST 예외를 던진다")
        void invalidRole() {
            given(userMapper.findByPhone(PHONE)).willReturn(null);

            assertThatThrownBy(() -> userService.create(createRequest("ADMIN")))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(exception.getMessage()).isEqualTo("role은 WARD 또는 GUARD여야 합니다.");
                });
        }

        @Test
        @DisplayName("생년월일 형식이 YYYYMMDD 8자리가 아니면 BAD_REQUEST 예외를 던진다")
        void invalidBirthDate() {
            given(userMapper.findByPhone(PHONE)).willReturn(null);
            UserCreateRequest request = createRequest("WARD");
            request.setBirthDate("1990-01-01");

            assertThatThrownBy(() -> userService.create(request))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(exception.getMessage()).isEqualTo("생년월일이 올바르지 않습니다.");
                });
        }

        private User wardUser() {
            User user = new User();
            user.setId(1L);
            user.setRole(Role.WARD);
            user.setPhone(PHONE);
            user.setName("홍길동");
            return user;
        }

        private User guardUser() {
            User user = new User();
            user.setId(1L);
            user.setRole(Role.GUARD);
            user.setPhone(PHONE);
            user.setName("홍길동");
            return user;
        }
    }

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("존재하면 UserResponse를 반환한다")
        void found() {
            User user = new User();
            user.setId(1L);
            user.setPhone(PHONE);
            given(userMapper.findById(1L)).willReturn(user);

            UserResponse response = userService.findById(1L);

            assertThat(response.getId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("존재하지 않으면 NOT_FOUND 예외를 던진다")
        void notFound() {
            given(userMapper.findById(1L)).willReturn(null);

            assertThatThrownBy(() -> userService.findById(1L))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                    assertThat(exception.getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("정상 수정한다")
        void success() {
            User user = new User();
            user.setId(1L);
            user.setName("old-name");
            user.setAvatarId(1);
            given(userMapper.findById(1L)).willReturn(user);

            UserUpdateRequest request = new UserUpdateRequest();
            request.setName("new-name");
            request.setAvatarId(4);

            UserResponse response = userService.update(1L, request);

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            then(userMapper).should().update(captor.capture());
            assertThat(captor.getValue().getName()).isEqualTo("new-name");
            assertThat(captor.getValue().getAvatarId()).isEqualTo(4);
            assertThat(response.getName()).isEqualTo("new-name");
            assertThat(response.getAvatarId()).isEqualTo(4);
        }

        @Test
        @DisplayName("존재하지 않으면 NOT_FOUND 예외를 던진다")
        void notFound() {
            given(userMapper.findById(1L)).willReturn(null);

            UserUpdateRequest request = new UserUpdateRequest();
            request.setName("new-name");

            assertThatThrownBy(() -> userService.update(1L, request))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                    assertThat(exception.getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("삭제되면 예외 없이 끝난다")
        void success() {
            given(userMapper.delete(1L)).willReturn(1);

            userService.delete(1L);

            then(userMapper).should().delete(1L);
        }

        @Test
        @DisplayName("대상이 없으면(0건 삭제) NOT_FOUND 예외를 던진다")
        void notFound() {
            given(userMapper.delete(1L)).willReturn(0);

            assertThatThrownBy(() -> userService.delete(1L))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                    assertThat(exception.getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("FCM 토큰")
    class FcmToken {

        private static final Long USER_ID = 1L;
        private static final String NEW_TOKEN = "device-token-abc";

        @Test
        @DisplayName("기기 토큰을 덮어쓴다 — 재발급될 때마다 갱신되는 값이라 이력을 남기지 않는다")
        void updateFcmToken_overwritesToken() {
            given(userMapper.findById(USER_ID)).willReturn(new User());

            userService.updateFcmToken(USER_ID, NEW_TOKEN);

            then(userMapper).should().updateFcmToken(USER_ID, NEW_TOKEN);
        }

        /**
         * 토큰은 앱 인스턴스 단위라, 앞선 사용자가 로그아웃하지 않고 떠난 브라우저에서 다음
         * 사용자가 로그인하면 같은 토큰이 나온다. 정리하지 않으면 두 사용자 행에 같은 토큰이
         * 남아 앞선 사용자의 거래 알림이 지금 기기를 쓰는 사람에게 간다.
         */
        @Test
        @DisplayName("등록 전에 같은 토큰을 쓰던 다른 사용자를 먼저 해제한다")
        void updateFcmToken_releasesTokenFromPreviousOwnerFirst() {
            given(userMapper.findById(USER_ID)).willReturn(new User());
            given(userMapper.clearFcmTokenFromOtherUsers(USER_ID, NEW_TOKEN)).willReturn(1);

            userService.updateFcmToken(USER_ID, NEW_TOKEN);

            InOrder inOrder = inOrder(userMapper);
            inOrder.verify(userMapper).clearFcmTokenFromOtherUsers(USER_ID, NEW_TOKEN);
            inOrder.verify(userMapper).updateFcmToken(USER_ID, NEW_TOKEN);
        }

        @Test
        @DisplayName("해제는 요청한 토큰이 현재 값과 같을 때만 지운다")
        void deleteFcmToken_clearsOnlyWhenTokenMatches() {
            given(userMapper.findById(USER_ID)).willReturn(new User());
            given(userMapper.clearFcmTokenIfMatches(USER_ID, NEW_TOKEN)).willReturn(1);

            userService.deleteFcmToken(USER_ID, NEW_TOKEN);

            then(userMapper).should().clearFcmTokenIfMatches(USER_ID, NEW_TOKEN);
            // 사용자 ID 만으로 지우는 경로가 남아 있으면 안 된다
            then(userMapper).should(never()).updateFcmToken(any(), any());
        }

        /**
         * 다른 기기로 로그인해 토큰이 교체된 뒤 옛 기기의 로그아웃이 뒤늦게 도착한 경우다.
         * 실패로 다루면 안 되고, 지금 쓰는 기기의 토큰을 지워서도 안 된다.
         */
        @Test
        @DisplayName("이미 교체된 토큰의 해제 요청은 아무것도 지우지 않고 성공으로 끝난다")
        void deleteFcmToken_succeedsWithoutClearingWhenTokenAlreadyReplaced() {
            given(userMapper.findById(USER_ID)).willReturn(new User());
            given(userMapper.clearFcmTokenIfMatches(USER_ID, "STALE_TOKEN")).willReturn(0);

            assertThatCode(() -> userService.deleteFcmToken(USER_ID, "STALE_TOKEN"))
                .doesNotThrowAnyException();

            then(userMapper).should(never()).updateFcmToken(any(), any());
        }

        @Test
        @DisplayName("탈퇴한 계정은 토큰을 등록하지 못한다 — 만료 전 JWT 로 다시 등록하는 경로를 막는다")
        void updateFcmToken_rejectsWithdrawnUser() {
            User withdrawn = new User();
            withdrawn.setStatus(UserStatus.WITHDRAWN);
            given(userMapper.findById(USER_ID)).willReturn(withdrawn);

            assertThatThrownBy(() -> userService.updateFcmToken(USER_ID, NEW_TOKEN))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                    assertThat(exception.getStatus()).isEqualTo(HttpStatus.FORBIDDEN));
            then(userMapper).should(never()).updateFcmToken(any(), any());
            then(userMapper).should(never()).clearFcmTokenFromOtherUsers(any(), any());
        }

        @Test
        @DisplayName("없는 사용자면 NOT_FOUND 를 던지고 토큰을 건드리지 않는다")
        void updateFcmToken_throwsWhenUserMissing() {
            given(userMapper.findById(USER_ID)).willReturn(null);

            assertThatThrownBy(() -> userService.updateFcmToken(USER_ID, NEW_TOKEN))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                    assertThat(exception.getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
            then(userMapper).should(never()).updateFcmToken(any(), anyString());
        }

        @Test
        @DisplayName("해제도 없는 사용자면 NOT_FOUND 를 던진다")
        void deleteFcmToken_throwsWhenUserMissing() {
            given(userMapper.findById(USER_ID)).willReturn(null);

            assertThatThrownBy(() -> userService.deleteFcmToken(USER_ID, NEW_TOKEN))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                    assertThat(exception.getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
            then(userMapper).should(never()).clearFcmTokenIfMatches(any(), any());
        }
    }
}
