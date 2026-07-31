package com.paywith.transfer.controller;


import com.paywith.common.ApiResponse;
import com.paywith.transfer.dto.RecipientInquiryRequest;
import com.paywith.transfer.dto.RecipientInquiryResponse;
import com.paywith.transfer.dto.TransferRequest;
import com.paywith.transfer.dto.TransferResponse;
import com.paywith.transfer.service.TransferService;
import io.swagger.annotations.ResponseHeader;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import springfox.documentation.annotations.ApiIgnore;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/ward/transfers")
@RequiredArgsConstructor
public class TransferController {

    private final TransferService transferService;

    @PostMapping("/recipient")
    public ApiResponse<RecipientInquiryResponse> inquireRecipient(
            @RequestBody RecipientInquiryRequest request
            ){
        RecipientInquiryResponse response = transferService.inquireRecipient(request);
        return ApiResponse.success(response);
    }

    @PostMapping
    public ResponseEntity<ApiResponse<TransferResponse>> transfer(
            @ApiIgnore @AuthenticationPrincipal Long userId,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody TransferRequest request
            ){
        TransferResponse response = transferService.transfer(userId, idempotencyKey, request);
        HttpStatus status = "HELD".equals(response.getStatus())
                ? HttpStatus.ACCEPTED //202 거래이상 보류
                : HttpStatus.CREATED; //201 송금완료
        return ResponseEntity.status(status).body(ApiResponse.success(response));
    }
}