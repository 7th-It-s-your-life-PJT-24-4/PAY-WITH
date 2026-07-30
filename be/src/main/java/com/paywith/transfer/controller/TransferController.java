package com.paywith.transfer.controller;


import com.paywith.common.ApiResponse;
import com.paywith.transfer.dto.RecipientInquiryRequest;
import com.paywith.transfer.dto.RecipientInquiryResponse;
import com.paywith.transfer.service.TransferService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
