package com.paywith.transfer.controller;


import com.paywith.common.ApiResponse;
import com.paywith.transaction.domain.TransactionStatus;
import com.paywith.transfer.dto.*;
import com.paywith.transfer.service.TransferService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ResponseHeader;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import springfox.documentation.annotations.ApiIgnore;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@Api(tags = "피보호자 송금")
@RestController
@RequestMapping("/api/ward/transfers")
@RequiredArgsConstructor
public class TransferController {

    private final TransferService transferService;

    @ApiOperation(
        value = "수취인 실명조회",
        notes = "송금 전 입력한 계좌의 예금주명을 조회한다. 피보호자(WARD)만 이용할 수 있고, "
            + "보호자와 페어링되어 있지 않으면 403. 계좌 정보와 일치하는 예금주를 찾지 못하면 404.")
    @PostMapping("/recipient")
    public ApiResponse<RecipientInquiryResponse> inquireRecipient(
            @ApiIgnore @AuthenticationPrincipal Long userId,
            @RequestBody RecipientInquiryRequest request
            ){
        RecipientInquiryResponse response = transferService.inquireRecipient(userId, request);
        return ApiResponse.success(response);
    }

    @ApiOperation(
        value = "송금",
        notes = "Idempotency-Key 헤더로 중복 요청을 방지한다. 동일 키로 이미 처리 중인 요청이 있으면 "
            + "409, 이전에 완료된 요청과 같은 내용이면 저장해둔 응답을 그대로 재반환한다. "
            + "FDS 평가 결과가 위험(DANGER)이면 송금을 보류하고 202로 응답한다. 블랙리스트에 걸린 "
            + "건은 보호자 승인 없이 차단되며 200으로 응답한다(생성된 것이 없으므로 201이 아니다). "
            + "그 외에는 송금을 완료하고 201로 응답한다. 어느 경우든 확정 상태는 본문 status 로 "
            + "판단한다.")
    @PostMapping
    public ResponseEntity<ApiResponse<TransferResponse>> transfer(
            @ApiIgnore @AuthenticationPrincipal Long userId,
            @ApiParam(value = "요청 재시도를 식별하는 클라이언트 생성 키", required = true, example = "3f29c1e0-...")
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody TransferRequest request
            ){
        TransferResponse response = transferService.transfer(userId, idempotencyKey, request);
        return ResponseEntity.status(httpStatusOf(response.getStatus()))
                .body(ApiResponse.success(response));
    }

    /**
     * 확정 상태를 HTTP 코드로 옮긴다. 새 상태가 생겼을 때 조용히 201(송금완료)로 흘러가지 않도록
     * 알고 있는 상태만 매핑하고 나머지는 200으로 둔다.
     */
    private HttpStatus httpStatusOf(TransactionStatus transferStatus) {
        if (transferStatus == TransactionStatus.HELD) {
            return HttpStatus.ACCEPTED;     //202 거래이상 보류(보호자 승인 대기)
        }
        if (transferStatus == TransactionStatus.COMPLETED) {
            return HttpStatus.CREATED;      //201 송금완료
        }
        return HttpStatus.OK;               //200 차단(BLOCKED) 등 — 이체가 일어나지 않은 종결
    }

    @ApiOperation(
        value = "송금 상대 이력 조회",
        notes = "피보호자가 과거에 송금한 적 있는 수취인 목록을 조회한다. 보호자와 페어링되어 있지 "
            + "않으면 403. sort 는 RECENT(기본값)/NAME 만 허용되며 그 외 값이거나 size 가 1~50 "
            + "범위를 벗어나면 400.")
    @GetMapping("recipient")
    public ApiResponse<RecipientHistoryListResponse> getRecipientHistory(
            @AuthenticationPrincipal Long userId,
            @ApiParam(value = "수취인 이름 검색어. 생략하면 전체 조회", example = "김")
            @RequestParam(required = false) String keyword,
            @ApiParam(value = "정렬 기준. RECENT(기본값) 또는 NAME", example = "RECENT")
            @RequestParam(required = false) String sort,
            @ApiParam(value = "조회 개수(1~50). 생략하면 20", example = "20")
            @RequestParam(required = false) Integer size
    ){
        RecipientHistoryListResponse response = transferService.getRecipientHistory(userId, keyword, sort, size);
        return ApiResponse.success(response);
    }

    @ApiOperation(
            value = "피보호자 대기 중 송금 취소",
            notes = "승인 대기(HELD) 중인 송금을 피보호자 본인이 직접 취소한다. "
                    + "다른 사람의 거래거나 송금이 아니면 404 TRANSFER_007, "
                    + "이미 완료·거절·취소된 송금이면 409 TRANSFER_008.")
    @PostMapping("/{id}/cancel")
    public ApiResponse<TransferCancelResponse> cancelHeldTransfer(
            @ApiIgnore @AuthenticationPrincipal Long userId,
            @ApiParam(value = "취소할 송금 거래 번호", required = true, example = "74")
            @PathVariable Long id
    ) {
        TransferCancelResponse response = transferService.cancelHeldTransfer(userId, id);
        return ApiResponse.success(response);
    }
}