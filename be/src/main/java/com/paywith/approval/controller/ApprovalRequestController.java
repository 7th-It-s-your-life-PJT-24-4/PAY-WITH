package com.paywith.approval.controller;

import com.paywith.approval.dto.ApprovalDecisionResponse;
import com.paywith.approval.dto.ApprovalRequestDetailResponse;
import com.paywith.approval.dto.ApprovalRequestSummaryResponse;
import com.paywith.approval.service.ApprovalRequestService;
import com.paywith.common.ApiResponse;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import springfox.documentation.annotations.ApiIgnore;

@Api(tags = "보호자 이상거래 승인")
@RestController
@RequestMapping("/api/approval-requests")
@RequiredArgsConstructor
public class ApprovalRequestController {

    private final ApprovalRequestService approvalRequestService;

    @ApiOperation(
        value = "승인 대기 목록",
        notes = "로그인한 보호자가 담당하는 시니어들의 승인 대기 건을 만료 임박 순으로 반환한다. "
            + "이미 처리했거나 만료된 건은 제외한다.")
    @GetMapping
    public ApiResponse<List<ApprovalRequestSummaryResponse>> findPending(
        @ApiIgnore @AuthenticationPrincipal Long guardId
    ) {
        return ApiResponse.success(approvalRequestService.findPending(guardId));
    }

    @ApiOperation(
        value = "승인 대기 건 상세",
        notes = "보류 사유(발동한 FDS 룰)를 함께 반환한다. 목록과 같이 승인 대기 건만 조회되므로, "
            + "이미 처리했거나 만료된 건과 담당하지 않는 시니어의 건은 404. "
            + "처리 결과는 승인/거절 응답으로 확인한다.")
    @GetMapping("/{approvalId}")
    public ApiResponse<ApprovalRequestDetailResponse> findDetail(
        @ApiIgnore @AuthenticationPrincipal Long guardId,
        @ApiParam(value = "승인요청 ID", required = true, example = "1")
        @PathVariable Long approvalId
    ) {
        return ApiResponse.success(approvalRequestService.findDetail(approvalId, guardId));
    }

    @ApiOperation(
        value = "승인",
        notes = "보류된 송금을 진행시킨다. 대기 상태가 아니거나 이미 만료된 건이면 409, "
            + "담당하지 않는 시니어의 건이면 404.")
    @PostMapping("/{approvalId}/approve")
    public ApiResponse<ApprovalDecisionResponse> approve(
        @ApiIgnore @AuthenticationPrincipal Long guardId,
        @ApiParam(value = "승인요청 ID", required = true, example = "1")
        @PathVariable Long approvalId
    ) {
        return ApiResponse.success(approvalRequestService.approve(approvalId, guardId));
    }

    @ApiOperation(
        value = "거절",
        notes = "보류된 송금을 취소시킨다. 대기 상태가 아니거나 이미 만료된 건이면 409, "
            + "담당하지 않는 시니어의 건이면 404.")
    @PostMapping("/{approvalId}/reject")
    public ApiResponse<ApprovalDecisionResponse> reject(
        @ApiIgnore @AuthenticationPrincipal Long guardId,
        @ApiParam(value = "승인요청 ID", required = true, example = "1")
        @PathVariable Long approvalId
    ) {
        return ApiResponse.success(approvalRequestService.reject(approvalId, guardId));
    }
}
