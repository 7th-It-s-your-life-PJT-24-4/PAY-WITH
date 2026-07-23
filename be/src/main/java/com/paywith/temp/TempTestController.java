package com.paywith.temp;

import com.paywith.external.openbanking.OpenBankingClient;
import com.paywith.external.openbanking.dto.RealNameInquiryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class TempTestController {

    private final OpenBankingClient openBankingClient;

    @GetMapping("/test/token")
    public String testToken() {
        return openBankingClient.getAccessToken();
    }

    @GetMapping("/test/verify")
    public RealNameInquiryResponse testVerify() {
        return openBankingClient.inquireRealName("004", "1101230000678", "880101");
    }

    @GetMapping("/test/token-raw")
    public Map<String, Object> testTokenRaw() {
        return openBankingClient.getRawTokenResponse();
    }
}