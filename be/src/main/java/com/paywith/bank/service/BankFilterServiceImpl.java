package com.paywith.bank.service;

import com.paywith.bank.domain.BankAccountRule;
import com.paywith.bank.dto.BankCandidateResponse;
import com.paywith.bank.dto.BankFilterResponse;
import com.paywith.bank.mapper.BankFilterMapper;
import com.paywith.exception.BusinessException;
import com.paywith.user.domain.Role;
import com.paywith.user.domain.User;
import com.paywith.user.mapper.UserMapper;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class BankFilterServiceImpl implements BankFilterService {

    private static final String ACCOUNT_NO_PATTERN = "\\d{1,20}";

    private final BankFilterMapper bankMapper;
    private final UserMapper userMapper;

    public BankFilterServiceImpl(BankFilterMapper bankMapper, UserMapper userMapper) {
        this.bankMapper = bankMapper;
        this.userMapper = userMapper;
    }

    @Override
    public BankFilterResponse filterBanks(Long wardId, String accountNo) {
        requireWardRole(wardId);
        requirePaired(wardId);

        if (accountNo == null || !accountNo.matches(ACCOUNT_NO_PATTERN)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "ACCOUNT_001", "계좌번호 형식이 올바르지 않습니다.");
        }

        // BankAccountRule 에 등록된 은행은 자릿수 범위 + prefix 조건으로 필터링한다.
        // Rule 이 없는 은행코드(향후 추가 은행 등)는 pass-through 로 항상 포함한다.
        List<BankCandidateResponse> matched = bankMapper.findAllActive().stream()
            .filter(bank -> BankAccountRule.findByBankCode(bank.getBankCode())
                .map(rule -> rule.matches(accountNo))
                .orElse(true))
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
}