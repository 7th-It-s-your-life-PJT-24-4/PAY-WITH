package com.paywith.approval.controller;

import com.paywith.approval.dto.ApprovalDecisionResponse;
import com.paywith.approval.dto.ApprovalRequestDetailResponse;
import com.paywith.approval.dto.ApprovalRequestSummaryResponse;
import com.paywith.approval.service.ApprovalRequestService;
import com.paywith.approval.service.ApprovalTransferFacade;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import springfox.documentation.annotations.ApiIgnore;

@Api(tags = "보호자 이상거래 승인")
@RestController
@RequestMapping("/api/approval-requests")
@RequiredArgsConstructor
public class ApprovalRequestController {

    private final ApprovalRequestService approvalRequestService;
    private final ApprovalTransferFacade approvalTransferFacade;

    @ApiOperation(
        value = "승인 대기 목록",
        notes = "로그인한 보호자가 담당(ACTIVE 연결)하는 시니어들의 승인 대기 건을 만료 임박 순(expiredAt 오름차순)으로 "
            + "반환한다. 이미 처리했거나 만료된 건은 제외한다. wardId 를 주면 그 피보호자 건만 조회한다. "
            + "역할 검사는 없어 담당이 아닌 wardId 를 주거나 보호자가 아닌 계정으로 호출해도 에러 없이 빈 배열이다. "
            + "wardId 가 비숫자면 500(code 없음).")
    @GetMapping
    public ApiResponse<List<ApprovalRequestSummaryResponse>> findPending(
        @ApiIgnore @AuthenticationPrincipal Long guardId,
        @ApiParam(value = "피보호자 ID. 생략하면 담당 피보호자 전체", example = "42")
        @RequestParam(required = false) Long wardId
    ) {
        return ApiResponse.success(approvalRequestService.findPending(guardId, wardId));
    }

    @ApiOperation(
        value = "종결된 승인요청 이력",
        notes = "본인이 처리한 승인·거절, 담당 피보호자의 취소, 자동 만료(기본 3시간) 이력을 종결 시각 최신순으로 "
            + "반환한다. status 는 APPROVED, REJECTED, CANCELED, EXPIRED 만 허용(대소문자·앞뒤 공백 무시)하며 그 외 값이면 "
            + "400 REQUEST_001 \"이력 상태는 APPROVED, REJECTED, CANCELED, EXPIRED만 조회할 수 있습니다.\". "
            + "status 를 생략하거나 wardId 가 비숫자면 500(code 없음). wardId 로 필터링할 수 있고, 담당 관계가 해제된 "
            + "시니어의 과거 기록은 제외된다. 피보호자 취소는 현재 응답자·응답 시각을 기록하지 않아 "
            + "status=CANCELED 는 항상 빈 배열이다.")
    @GetMapping("/history")
    public ApiResponse<List<ApprovalRequestSummaryResponse>> findHistory(
        @ApiIgnore @AuthenticationPrincipal Long guardId,
        @ApiParam(value = "이력 상태(대소문자·앞뒤 공백 무시). CANCELED 는 현재 구현상 항상 빈 배열",
            required = true, example = "EXPIRED",
            allowableValues = "APPROVED,REJECTED,CANCELED,EXPIRED")
        @RequestParam String status,
        @ApiParam(value = "피보호자 ID. 생략하면 담당 피보호자 전체", example = "42")
        @RequestParam(required = false) Long wardId
    ) {
        return ApiResponse.success(
            approvalRequestService.findHistory(guardId, wardId, status));
    }

    @ApiOperation(
        value = "승인 대기 건 상세",
        notes = "보류 사유(발동한 FDS 룰)를 점수 큰 순으로 함께 반환한다. 목록과 같이 승인 대기 건만 조회되므로, "
            + "존재하지 않는 ID·이미 처리했거나 만료된 건·담당하지 않는 시니어의 건은 모두 "
            + "404 \"승인요청을 찾을 수 없습니다.\"(응답에 code 없음). approvalId 가 비숫자면 500(code 없음). "
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
        notes = "보류된 송금을 승인하고 이어서 실제 송금까지 실행한다. 존재하지 않거나 담당하지 않는 시니어의 건이면 "
            + "404 \"승인요청을 찾을 수 없습니다.\", 대기 상태가 아니거나 이미 만료된 건이면 "
            + "409 \"이미 처리되었거나 만료된 승인요청입니다.\"(둘 다 응답에 code 없음). approvalId 가 비숫자면 500. "
            + "승인이 확정된 뒤의 송금 실패는 예외로 던지지 않고 200 으로 응답하며 transfer.status=FAILED 와 "
            + "failureReason 에 담긴다. 잔액 부족 같은 입금 전 실패는 거래가 FAILED 로 종결되지만, 입금 호출 이후의 "
            + "실패나 원인 불명 오류는 거래가 APPROVED 로 남은 채 FAILED 로 응답하므로(성패 미확정) 재송금 전 확인이 "
            + "필요하다. respondedAt·transfer.completedAt 은 서버 시각이라 마이크로초까지 포함될 수 있다.")
    @PostMapping("/{approvalId}/approve")
    public ApiResponse<ApprovalDecisionResponse> approve(
        @ApiIgnore @AuthenticationPrincipal Long guardId,
        @ApiParam(value = "승인요청 ID", required = true, example = "1")
        @PathVariable Long approvalId
    ) {
        return ApiResponse.success(approvalTransferFacade.approveAndTransfer(approvalId, guardId));
    }

    @ApiOperation(
        value = "거절",
        notes = "보류된 송금을 거절 처리한다(거래는 REJECTED 로 종결, 피보호자에게 거절 알림 발송). "
            + "존재하지 않거나 담당하지 않는 시니어의 건이면 404 \"승인요청을 찾을 수 없습니다.\", "
            + "대기 상태가 아니거나 이미 만료된 건이면 409 \"이미 처리되었거나 만료된 승인요청입니다.\"(둘 다 응답에 "
            + "code 없음). approvalId 가 비숫자면 500. transfer 는 항상 null 이며 respondedAt 은 서버 시각이라 "
            + "마이크로초까지 포함될 수 있다.")
    @PostMapping("/{approvalId}/reject")
    public ApiResponse<ApprovalDecisionResponse> reject(
        @ApiIgnore @AuthenticationPrincipal Long guardId,
        @ApiParam(value = "승인요청 ID", required = true, example = "1")
        @PathVariable Long approvalId
    ) {
        return ApiResponse.success(approvalRequestService.reject(approvalId, guardId));
    }
}
