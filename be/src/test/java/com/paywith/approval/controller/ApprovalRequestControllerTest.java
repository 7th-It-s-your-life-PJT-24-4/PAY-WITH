package com.paywith.approval.controller;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.paywith.approval.domain.ApprovalRequestView;
import com.paywith.approval.dto.ApprovalDecisionResultResponse;
import com.paywith.approval.dto.ApprovalRequestSummaryResponse;
import com.paywith.approval.service.ApprovalRequestService;
import com.paywith.approval.service.ApprovalTransferFacade;
import com.paywith.exception.GlobalExceptionHandler;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class ApprovalRequestControllerTest {

    private static final long GUARD_ID = 10L;
    private static final long WARD_ID = 42L;
    private static final long APPROVAL_ID = 100L;

    private MockMvc mockMvc;

    @Mock
    private ApprovalRequestService approvalRequestService;

    @Mock
    private ApprovalTransferFacade approvalTransferFacade;

    private final Authentication guardAuthentication = new UsernamePasswordAuthenticationToken(
        GUARD_ID,
        null,
        Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        SecurityContextHolder.getContext().setAuthentication(guardAuthentication);
        mockMvc = MockMvcBuilders
            .standaloneSetup(
                new ApprovalRequestController(approvalRequestService, approvalTransferFacade))
            .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private ApprovalRequestView processedView(String status) {
        ApprovalRequestView view = new ApprovalRequestView();
        view.setApprovalId(APPROVAL_ID);
        view.setTransactionId(500L);
        view.setWardId(WARD_ID);
        view.setWardName("김시니어");
        view.setAmount(new BigDecimal("2000000"));
        view.setHolderName("박수취");
        view.setBankName("신한은행");
        view.setAccountNo("110234567890");
        view.setRiskLevel("DANGER");
        view.setTotalScore(64);
        view.setStatus(status);
        view.setRequestedAt(LocalDateTime.of(2026, 8, 5, 9, 0));
        view.setExpiredAt(LocalDateTime.of(2026, 8, 5, 12, 0));
        view.setRespondedAt(LocalDateTime.of(2026, 8, 5, 9, 10));
        view.setTransactionStatus("REJECTED");
        return view;
    }

    @Test
    void findDecisionHistory_returnsFilteredListAndPassesPrincipal() throws Exception {
        given(approvalRequestService.findDecisionHistory(GUARD_ID, WARD_ID, "REJECTED"))
            .willReturn(List.of(new ApprovalRequestSummaryResponse(processedView("REJECTED"))));

        mockMvc.perform(get("/api/approval-requests/history")
                .param("status", "REJECTED")
                .param("wardId", String.valueOf(WARD_ID)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data[0].approvalId").value(APPROVAL_ID))
            .andExpect(jsonPath("$.data[0].status").value("REJECTED"))
            .andExpect(jsonPath("$.data[0].respondedAt").isArray());

        then(approvalRequestService).should()
            .findDecisionHistory(GUARD_ID, WARD_ID, "REJECTED");
    }

    @Test
    void findDecisionResult_returnsDatabaseBackedSnapshotShape() throws Exception {
        ApprovalDecisionResultResponse response =
            new ApprovalDecisionResultResponse(processedView("REJECTED"), List.of());
        given(approvalRequestService.findDecisionResult(APPROVAL_ID, GUARD_ID))
            .willReturn(response);

        mockMvc.perform(get("/api/approval-requests/100/result"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.detail.approvalId").value(APPROVAL_ID))
            .andExpect(jsonPath("$.data.detail.wardId").value(WARD_ID))
            .andExpect(jsonPath("$.data.decision.status").value("REJECTED"))
            .andExpect(jsonPath("$.data.decision.transfer").isEmpty());

        then(approvalRequestService).should().findDecisionResult(APPROVAL_ID, GUARD_ID);
    }
}
