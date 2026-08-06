package com.paywith.approval.controller;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.paywith.approval.domain.ApprovalRequestView;
import com.paywith.approval.dto.WardApprovalDetailResponse;
import com.paywith.approval.service.WardApprovalRequestService;
import com.paywith.exception.BusinessException;
import com.paywith.exception.GlobalExceptionHandler;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class WardApprovalRequestControllerTest {

    private static final long WARD_ID = 9001L;
    private static final long APPROVAL_ID = 100L;

    private MockMvc mockMvc;

    @Mock
    private WardApprovalRequestService wardApprovalRequestService;

    private final Authentication wardAuthentication = new UsernamePasswordAuthenticationToken(
        WARD_ID,
        null,
        Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
    );

    /**
     * 컨트롤러가 {@code @AuthenticationPrincipal} 로 대상을 정하므로, 리졸버를 등록하고
     * SecurityContext 에 인증을 올려야 실제 경로와 같아진다. 리졸버는 요청의 principal 이 아니라
     * SecurityContextHolder 를 읽는다.
     */
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        SecurityContextHolder.getContext().setAuthentication(wardAuthentication);
        mockMvc = MockMvcBuilders
            .standaloneSetup(new WardApprovalRequestController(wardApprovalRequestService))
            .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private WardApprovalDetailResponse response() {
        ApprovalRequestView view = new ApprovalRequestView();
        view.setApprovalId(APPROVAL_ID);
        view.setTransactionId(500L);
        view.setType("TRANSFER_OUT");
        view.setAmount(new BigDecimal("2000000"));
        view.setMemo("생활비");
        view.setHolderName("박수취");
        view.setBankName("신한은행");
        view.setAccountNo("110234567890");
        view.setRiskLevel("DANGER");
        view.setRequestedAt(LocalDateTime.now());
        view.setExpiredAt(LocalDateTime.now().plusMinutes(30));
        return new WardApprovalDetailResponse(view);
    }

    @Test
    void findDetailByWard_returnsApiResponseEnvelope() throws Exception {
        given(wardApprovalRequestService.findDetailByWard(APPROVAL_ID, WARD_ID))
            .willReturn(response());

        mockMvc.perform(get("/api/ward/approval-requests/100"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.approvalId").value(100))
            .andExpect(jsonPath("$.data.type").value("TRANSFER_OUT"))
            .andExpect(jsonPath("$.data.amount").value(2000000))
            .andExpect(jsonPath("$.data.memo").value("생활비"))
            .andExpect(jsonPath("$.data.riskLevel").value("DANGER"));
    }

    // 보류 사유와 피보호자 식별 정보는 응답 계약에 없다
    @Test
    void findDetailByWard_omitsRuleHitsAndWardIdentity() throws Exception {
        given(wardApprovalRequestService.findDetailByWard(APPROVAL_ID, WARD_ID))
            .willReturn(response());

        mockMvc.perform(get("/api/ward/approval-requests/100"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.ruleHits").doesNotExist())
            .andExpect(jsonPath("$.data.totalScore").doesNotExist())
            .andExpect(jsonPath("$.data.wardId").doesNotExist())
            .andExpect(jsonPath("$.data.wardName").doesNotExist());
    }

    // 대상을 파라미터로 받지 않고 인증 주체로만 정한다. 남의 ID 를 넣을 자리가 없다
    @Test
    void findDetailByWard_passesAuthenticatedPrincipalAsWardId() throws Exception {
        given(wardApprovalRequestService.findDetailByWard(APPROVAL_ID, WARD_ID))
            .willReturn(response());

        mockMvc.perform(get("/api/ward/approval-requests/100"))
            .andExpect(status().isOk());

        then(wardApprovalRequestService).should().findDetailByWard(APPROVAL_ID, WARD_ID);
    }

    @Test
    void findDetailByWard_notFoundIsReturnedAs404() throws Exception {
        given(wardApprovalRequestService.findDetailByWard(APPROVAL_ID, WARD_ID))
            .willThrow(new BusinessException(HttpStatus.NOT_FOUND, "승인요청을 찾을 수 없습니다."));

        mockMvc.perform(get("/api/ward/approval-requests/100"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.message").value("승인요청을 찾을 수 없습니다."));
    }
}
