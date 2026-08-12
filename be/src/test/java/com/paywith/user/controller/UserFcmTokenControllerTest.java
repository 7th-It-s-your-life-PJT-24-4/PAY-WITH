package com.paywith.user.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.paywith.exception.GlobalExceptionHandler;
import com.paywith.user.service.UserService;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * 토큰 등록·해제는 기기가 들어오는 유일한 입구다. 경로 변수 대신 인증 주체에서 사용자를 얻는지,
 * 길이 제한이 실제로 걸리는지를 확인한다.
 */
@DisplayName("FCM 토큰 등록·해제 API")
class UserFcmTokenControllerTest {

    private static final long USER_ID = 7L;
    private static final long OTHER_USER_ID = 99L;

    private MockMvc mockMvc;

    @Mock
    private UserService userService;

    private final Authentication authentication = new UsernamePasswordAuthenticationToken(
        USER_ID, null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders
            .standaloneSetup(new UserController(userService))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
    }

    @Test
    @DisplayName("등록은 인증된 본인 ID 로만 위임한다 — 남의 토큰을 덮어쓸 경로가 없어야 한다")
    void updateFcmToken_usesAuthenticatedUserId() throws Exception {
        mockMvc.perform(put("/api/users/me/fcm-token")
                .principal(authentication)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"fcmToken\":\"device-token-abc\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));

        then(userService).should().updateFcmToken(USER_ID, "device-token-abc");
        then(userService).should(never()).updateFcmToken(eq(OTHER_USER_ID), anyString());
    }

    @Test
    @DisplayName("해제는 본인 ID 와 요청한 토큰을 함께 넘긴다 — 어느 기기의 해제인지 구분해야 한다")
    void deleteFcmToken_passesAuthenticatedUserIdAndToken() throws Exception {
        mockMvc.perform(delete("/api/users/me/fcm-token")
                .principal(authentication)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"fcmToken\":\"device-token-abc\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));

        then(userService).should().deleteFcmToken(USER_ID, "device-token-abc");
    }

    @Test
    @DisplayName("해제 요청에 토큰이 없으면 400 — 토큰 없이 지우면 다른 기기 것이 지워진다")
    void deleteFcmToken_rejectsMissingToken() throws Exception {
        mockMvc.perform(delete("/api/users/me/fcm-token")
                .principal(authentication)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isBadRequest());

        then(userService).should(never()).deleteFcmToken(anyLong(), any());
    }

    @Test
    @DisplayName("빈 토큰은 400 으로 막고 서비스를 부르지 않는다")
    void updateFcmToken_rejectsBlankToken() throws Exception {
        mockMvc.perform(put("/api/users/me/fcm-token")
                .principal(authentication)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"fcmToken\":\"\"}"))
            .andExpect(status().isBadRequest());

        then(userService).should(never()).updateFcmToken(anyLong(), any());
    }

    /**
     * users.fcm_token 이 VARCHAR(255) 라 넘치면 DB 가 1406 을 던져 500 이 된다. 여기서 걸러
     * 400 으로 돌려주는지 고정해 둔다 — 이 검증을 지우면 원인을 알 수 없는 500 이 된다.
     */
    @Test
    @DisplayName("255자를 넘는 토큰은 400 으로 막는다 — 컬럼 길이를 넘겨 500 이 되기 전에")
    void updateFcmToken_rejectsTokenLongerThanColumn() throws Exception {
        String tooLong = "a".repeat(256);

        mockMvc.perform(put("/api/users/me/fcm-token")
                .principal(authentication)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"fcmToken\":\"" + tooLong + "\"}"))
            .andExpect(status().isBadRequest());

        then(userService).should(never()).updateFcmToken(anyLong(), any());
    }

    @Test
    @DisplayName("255자 토큰은 통과한다 — 경계값이 막히면 안 된다")
    void updateFcmToken_acceptsTokenAtColumnLimit() throws Exception {
        String atLimit = "a".repeat(255);

        mockMvc.perform(put("/api/users/me/fcm-token")
                .principal(authentication)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"fcmToken\":\"" + atLimit + "\"}"))
            .andExpect(status().isOk());

        then(userService).should().updateFcmToken(USER_ID, atLimit);
    }
}
