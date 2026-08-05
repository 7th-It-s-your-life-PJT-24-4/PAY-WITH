package com.paywith.bank.controller;

import com.paywith.bank.dto.BankResponse;
import com.paywith.common.ApiResponse;
import com.paywith.recipient.mapper.BankMapper;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/banks")
public class BankController {

    private final BankMapper bankMapper;

    public BankController(BankMapper bankMapper) {
        this.bankMapper = bankMapper;
    }

    @GetMapping
    public ApiResponse<List<BankResponse>> findAllActive() {
        return ApiResponse.success(bankMapper.findAllActive());
    }
}
