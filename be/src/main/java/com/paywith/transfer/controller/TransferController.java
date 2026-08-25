package com.paywith.transfer.controller;


import com.paywith.common.ApiResponse;
import com.paywith.transaction.domain.TransactionStatus;
import com.paywith.transfer.dto.*;
import com.paywith.transfer.service.TransferService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
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
        notes = "송금 전 입력한 계좌의 예금주명을 조회한다. 피보호자(WARD)가 아니면 403 AUTH_004, "
            + "보호자와 페어링되어 있지 않으면 403 WARD_001. 계좌 정보와 일치하는 예금주를 찾지 못하면 "
            + "404 RECIPIENT_001 (현재 실명조회 목 구현이 항상 성공을 돌려주므로 미발생 — 실연동 시 계약). "
            + "bankCode·accountNo 는 서버 검증이 없어 누락되거나 임의 문자열이어도 200 으로 응답하며, "
            + "응답의 bankCode·accountNo 는 요청값을 그대로 돌려준다. JSON 파싱 실패는 500.")
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
        notes = "Idempotency-Key 헤더로 중복 요청을 방지한다(헤더 누락 시 400 이 아니라 500). 409는 세 가지 — "
            + "동일 키가 아직 처리 중이면 IDEMPOTENCY_002, 동일 키에 다른 요청 내용이면 TRANSFER_005, "
            + "입금 호출 이후 실패(FAILED)로 확정된 키를 재시도하면 코드 없는 409. 요청 동일성은 "
            + "bankCode·accountNo·amount·memo 로 판단하며 transferPin 은 제외한다. 완료된 키에 같은 내용으로 "
            + "재요청하면 저장해둔 응답을 HTTP 상태(201/202/200)까지 최초와 동일하게 재반환한다. "
            + "FDS 평가 결과가 위험(DANGER)이면 송금을 보류하고 202로 응답한다. 블랙리스트에 걸린 "
            + "건은 보호자 승인 없이 차단되며 200으로 응답한다(생성된 것이 없으므로 201이 아니다). "
            + "그 외에는 송금을 완료하고 201로 응답한다. 어느 경우든 확정 상태는 본문 status 로 "
            + "판단한다. 그 밖의 오류(검사 순서대로): amount 누락·0 이하 400 REQUEST_001(message 는 "
            + "\"amount: 송금 금액은 필수입니다.\" 또는 \"amount: 송금 금액은 0보다 커야 합니다.\"), "
            + "지갑 없음 400(코드 없음, message \"지갑을 찾을 수 없습니다\" — 역할 검사보다 먼저 수행), "
            + "피보호자가 아니면 403 AUTH_004, 페어링 미완료 403 WARD_001, "
            + "PIN 불일치 400 TRANSFER_002(transferPin 누락·null 이면 500), 수취 계좌 미확인 "
            + "404 RECIPIENT_001(현재 실명조회 목 구현으로 미발생), 잔액 부족 422 WALLET_003. "
            + "amount 비숫자 등 JSON 파싱 실패와 bankCode 누락(DB 제약)은 500, 입금 호출 이후 실패는 "
            + "코드 없는 500 이며 이후 같은 키 재요청은 위의 코드 없는 409 로 이어진다.")
    @PostMapping
    public ResponseEntity<ApiResponse<TransferResponse>> transfer(
            @ApiIgnore @AuthenticationPrincipal Long userId,
            @ApiParam(value = "요청 재시도를 식별하는 클라이언트 생성 키(UUID 권장). 사용자별로 분리 저장되며 "
                + "처리 중 5분·완료/실패 후 24시간 유지된다. 누락 시 500",
                required = true, example = "3f29c1e0-7b2a-4c8d-9e1f-0a1b2c3d4e5f")
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
        notes = "피보호자가 과거에 송금한 적 있는 수취인 목록을 조회한다(Notion 명세의 \"송금 내역 조회\" 페이지에 "
            + "해당 — 거래 단위 송금 내역은 GET /api/ward/transactions 가 담당). 피보호자가 아니면 403 AUTH_004, "
            + "보호자와 페어링되어 있지 않으면 403 WARD_001. sort 는 RECENT(기본값)/NAME 만 허용되며 그 외 값이거나 "
            + "size 가 1~50 범위를 벗어나면 400 RECIPIENT_004. size 가 숫자가 아니면 500. keyword 는 예금주명 또는 "
            + "계좌번호 부분 일치(길이 검증 없음). 항목의 safeAccountId 는 안전계좌 등록 시 recipientId 와 같은 값이고, "
            + "banks 테이블에 없는 은행코드의 수취인은 목록에서 빠진다.")
    @GetMapping("recipient")
    public ApiResponse<RecipientHistoryListResponse> getRecipientHistory(
            @ApiIgnore @AuthenticationPrincipal Long userId,
            @ApiParam(value = "예금주명 또는 계좌번호 부분 일치 검색어(서버 길이 검증 없음). 생략하면 전체 조회", example = "김")
            @RequestParam(required = false) String keyword,
            @ApiParam(value = "정렬 기준. RECENT(기본값, 최근 송금순) 또는 NAME(예금주명순). 그 외 값은 400 RECIPIENT_004",
                allowableValues = "RECENT,NAME", example = "RECENT")
            @RequestParam(required = false) String sort,
            @ApiParam(value = "조회 개수(1~50). 생략하면 20. 범위 밖은 400 RECIPIENT_004, 숫자가 아니면 500",
                allowableValues = "range[1, 50]", example = "20")
            @RequestParam(required = false) Integer size
    ){
        RecipientHistoryListResponse response = transferService.getRecipientHistory(userId, keyword, sort, size);
        return ApiResponse.success(response);
    }

    @ApiOperation(
            value = "피보호자 대기 중 송금 취소",
            notes = "승인 대기(HELD) 중인 송금을 피보호자 본인이 직접 취소한다. 피보호자가 아니면 403 AUTH_004"
                    + "(페어링 검사는 없음). 거래가 없거나 다른 사람의 거래거나 송금(TRANSFER_OUT)이 아니면 "
                    + "404 TRANSFER_007, HELD 가 아니면(완료·거절·취소·차단 등, 경합으로 갱신 0건인 경우 포함) "
                    + "409 TRANSFER_008. 취소 시 PENDING 승인 요청도 함께 CANCELED 로 종결되며 응답 status 는 "
                    + "CANCELED. id 가 숫자가 아니면 500.")
    @PostMapping("/{id}/cancel")
    public ApiResponse<TransferCancelResponse> cancelHeldTransfer(
            @ApiIgnore @AuthenticationPrincipal Long userId,
            @ApiParam(value = "취소할 송금 거래 번호(숫자가 아니면 500)", required = true, example = "74")
            @PathVariable Long id
    ) {
        TransferCancelResponse response = transferService.cancelHeldTransfer(userId, id);
        return ApiResponse.success(response);
    }
}