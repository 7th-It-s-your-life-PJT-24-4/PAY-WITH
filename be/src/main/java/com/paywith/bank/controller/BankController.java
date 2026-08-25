package com.paywith.bank.controller;

import com.paywith.bank.dto.BankResponse;
import com.paywith.common.ApiResponse;
import com.paywith.recipient.mapper.BankMapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Api(tags = "은행")
@RestController
@RequestMapping("/api/banks")
public class BankController {

    private final BankMapper bankMapper;

    public BankController(BankMapper bankMapper) {
        this.bankMapper = bankMapper;
    }

    @ApiOperation(
        value = "은행 목록 조회",
        notes = "계좌 등록 화면의 은행 선택지로 쓰이는 활성 은행 목록을 은행 코드 오름차순으로 반환한다. "
            + "인증만 필요하며 역할 제한은 없다.")
    @GetMapping
    public ApiResponse<List<BankResponse>> findAllActive() {
        return ApiResponse.success(bankMapper.findAllActive());
    }
}
