package com.paywith.external.fraudcheck;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** 신고 계좌 목록을 설정에서 {@code 은행코드:계좌번호} 형태로 받는다. */
@Component
public class MockFraudAccountClient implements FraudAccountClient {

    private final Set<String> reportedAccounts;

    public MockFraudAccountClient(
        @Value("${fds.fraud-check.mock-accounts:}") String[] mockAccounts
    ) {
        this.reportedAccounts = Arrays.stream(mockAccounts)
            .map(String::trim)
            .filter(entry -> !entry.isEmpty())
            .collect(Collectors.toCollection(HashSet::new));
    }

    @Override
    public boolean isReportedAsFraud(String bankCode, String accountNo) {
        if (bankCode == null || accountNo == null) {
            return false;
        }
        return reportedAccounts.contains(key(bankCode, accountNo));
    }

    private String key(String bankCode, String accountNo) {
        return bankCode.trim() + ":" + accountNo.replace("-", "").trim();
    }
}
