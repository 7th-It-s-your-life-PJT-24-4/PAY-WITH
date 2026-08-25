package com.paywith.transaction.controller;

import com.paywith.common.ApiResponse;
import com.paywith.transaction.dto.GuardTransactionDetailResponse;
import com.paywith.transaction.dto.GuardTransactionHistoryListResponse;
import com.paywith.transaction.service.TransactionHistoryService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

@Api(tags = "보호자 거래내역")
@RestController
@RequestMapping("/api/guard/wards/{wardId}/transactions")
@RequiredArgsConstructor
public class GuardTransactionController {

    private final TransactionHistoryService transactionHistoryService;

    @ApiOperation(
            value = "피보호자 거래내역 조회 (보호자)",
            notes = "보호자가 담당 피보호자의 거래내역을 최신순(createdAt 내림차순)으로 조회한다. "
                    + "역할 검사는 없고 ACTIVE 담당 관계만 확인하므로 담당이 아니면(피보호자 계정이 호출한 경우 포함) "
                    + "404 LINK_001. 담당 확인이 값 검증보다 먼저다. "
                    + "보호자가 대신 실행한 거래(대리 충전, initiated_by 있음)는 이 목록에서 제외되고"
                    + "(충전 이력은 보호자 충전 내역 API 사용) 피보호자 본인 충전은 CHARGE 로 포함된다. "
                    + "type(CHARGE·TRANSFER·PAYMENT)·riskLevel(SAFE·CAUTION·DANGER) 허용값 위반(대소문자 구분), "
                    + "page 음수, size 1~100 이탈은 400 REQUEST_001 \"요청 값이 올바르지 않습니다.\". "
                    + "page 생략 시 0, size 생략 시 20. 비숫자 wardId·page·size 는 500. "
                    + "시각 필드는 오프셋 없는 ISO-8601.")
    @GetMapping
    public ApiResponse<GuardTransactionHistoryListResponse> findWardTransactions(
            @ApiIgnore @AuthenticationPrincipal Long guardId,
            @ApiParam(value = "피보호자 ID", required = true, example = "1")
            @PathVariable Long wardId,
            @ApiParam(value = "거래 유형 필터. 생략하면 전체. 대소문자 구분",
                    allowableValues = "CHARGE,TRANSFER,PAYMENT", example = "TRANSFER")
            @RequestParam(required = false) String type,
            @ApiParam(value = "위험 등급 필터. 생략하면 전체. 평가 기록이 없는 거래는 어떤 값으로도 걸리지 않는다. 대소문자 구분",
                    allowableValues = "SAFE,CAUTION,DANGER", example = "DANGER")
            @RequestParam(required = false) String riskLevel,
            @ApiParam(value = "페이지 번호(0부터 시작). 생략 시 0, 음수면 400", example = "0")
            @RequestParam(required = false) Integer page,
            @ApiParam(value = "페이지당 거래 수(1~100). 생략 시 20", example = "20")
            @RequestParam(required = false) Integer size
    ) {
        return ApiResponse.success(
                transactionHistoryService.findWardTransactions(guardId, wardId, type, riskLevel, page, size)
        );
    }

    @ApiOperation(
            value = "피보호자 거래내역 상세 조회 (보호자)",
            notes = "보호자가 담당 피보호자의 거래 상세를 조회한다. "
                    + "역할 검사는 없고 ACTIVE 담당 관계만 확인하므로 담당이 아니면 404 LINK_001, "
                    + "거래가 없거나 다른 피보호자 것이면 404 TRANSACTION_003. "
                    + "목록과 달리 initiated_by 필터가 없어 대리 충전 건도 id 로는 조회된다. "
                    + "riskAnalysis 는 type 이 CHARGE 이거나 위험 평가 기록이 없으면 null(SAFE 라도 평가 기록이 있으면 채워짐). "
                    + "비숫자 wardId·id 는 500. 시각 필드는 오프셋 없는 ISO-8601.")
    @GetMapping("/{id}")
    public ApiResponse<GuardTransactionDetailResponse> findWardTransactionDetail(
            @ApiIgnore @AuthenticationPrincipal Long guardId,
            @ApiParam(value = "피보호자 ID", required = true, example = "1")
            @PathVariable Long wardId,
            @ApiParam(value = "거래 식별자", required = true, example = "1031")
            @PathVariable Long id
    ) {
        return ApiResponse.success(
                transactionHistoryService.findWardTransactionDetail(guardId, wardId, id)
        );
    }
}