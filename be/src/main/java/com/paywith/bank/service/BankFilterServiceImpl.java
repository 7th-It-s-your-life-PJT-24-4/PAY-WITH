package com.paywith.bank.service;

import com.paywith.bank.domain.Bank;
import com.paywith.bank.dto.BankCandidateResponse;
import com.paywith.bank.dto.BankFilterResponse;
import com.paywith.bank.mapper.BankFilterMapper;
import com.paywith.exception.BusinessException;
import com.paywith.user.domain.Role;
import com.paywith.user.domain.User;
import com.paywith.user.mapper.UserMapper;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class BankFilterServiceImpl implements BankFilterService {

    private static final String ACCOUNT_NO_PATTERN = "\\d{1,20}";

    private final BankFilterMapper bankMapper;
    private final UserMapper userMapper;
    private final Map<String, Integer> accountLengthByBankCode;

    public BankFilterServiceImpl(
        BankFilterMapper bankMapper,
        UserMapper userMapper,
        @Value("${bank.account-length-mock:}") String[] accountLengthEntries
    ) {
        this.bankMapper = bankMapper;
        this.userMapper = userMapper;
        this.accountLengthByBankCode = parseAccountLengths(accountLengthEntries);
    }

    @Override
    public BankFilterResponse filterBanks(Long wardId, String accountNo) {
        requireWardRole(wardId);
        requirePaired(wardId);

        if (accountNo == null || !accountNo.matches(ACCOUNT_NO_PATTERN)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "ACCOUNT_001", "계좌번호 형식이 올바르지 않습니다.");
        }

        int length = accountNo.length();
        List<BankCandidateResponse> matched = bankMapper.findAllActive().stream()
            .filter(bank -> accountLengthByBankCode.getOrDefault(bank.getBankCode(), -1) == length)
            .map(bank -> new BankCandidateResponse(bank.getBankCode(), bank.getBankName()))
            .collect(Collectors.toList());

        return new BankFilterResponse(matched);
    }

    private void requireWardRole(Long wardId) {
        User user = userMapper.findById(wardId);
        if (user == null || user.getRole() != Role.WARD) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "AUTH_004", "피보호자만 접근할 수 있습니다.");
        }
    }

    private void requirePaired(Long wardId) {
        if (!bankMapper.existsActivePairing(wardId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "WARD_001", "페어링 완료 후 이용할 수 있습니다.");
        }
    }

    private Map<String, Integer> parseAccountLengths(String[] entries) {
        Map<String, Integer> lengths = new HashMap<>();
        Arrays.stream(entries)
            .map(String::trim)
            .filter(entry -> !entry.isEmpty())
            .forEach(entry -> {
                String[] parts = entry.split(":");
                lengths.put(parts[0].trim(), Integer.parseInt(parts[1].trim()));
            });
        return lengths;
    }
}