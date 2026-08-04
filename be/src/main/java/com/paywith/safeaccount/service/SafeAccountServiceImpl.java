package com.paywith.safeaccount.service;

import com.paywith.exception.BusinessException;
import com.paywith.external.openbanking.OpenBankingClient;
import com.paywith.external.openbanking.dto.RealNameInquiryResponse;
import com.paywith.guard.service.GuardService;
import com.paywith.recipient.domain.Recipient;
import com.paywith.recipient.mapper.BankMapper;
import com.paywith.recipient.mapper.RecipientMapper;
import com.paywith.safeaccount.dto.*;
import com.paywith.safeaccount.mapper.SafeAccountMapper;
import com.paywith.user.domain.Role;
import com.paywith.user.domain.User;
import com.paywith.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SafeAccountServiceImpl implements SafeAccountService{

    private static final int MAX_ALIAS_LENGTH = 50;

    private final UserMapper userMapper;
    private final RecipientMapper recipientMapper;
    private final BankMapper bankMapper;
    private final SafeAccountMapper safeAccountMapper;
    private final GuardService guardService;
    private final OpenBankingClient openBankingClient;

    @Override
    @Transactional
    public SafeAccountResponse registerByWard(Long wardId, SafeAccountRegisterRequest request) {
        // 1. 피보호자인지 확인
        User user = userMapper.findById(wardId);
        if(user.getRole() != Role.WARD){
            throw new BusinessException(HttpStatus.FORBIDDEN,"AUTH_004", "피보호자만 접근할 수 있습니다.");
        }

        // 2. 보호자 페어링 확인
        if(!recipientMapper.existsActivePairing(wardId)){
            throw new BusinessException(HttpStatus.FORBIDDEN, "WARD_001", "페어링 완료 후 이용할 수 있습니다.");
        }

        // 3. 별칭 검증 및 정규화 과정
        String alias = normalizeAlias(request.getAccountAlias());

        // 4. 내가 등록한 수취인이 맞는지 recipientId 확인
        Recipient recipient = (request.getRecipientId() == null)
                ? null
                : recipientMapper.findById(request.getRecipientId());
        if (recipient == null || !recipient.getWardId().equals(wardId)) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "RECIPIENT_003", "송금 이력이 있는 수취인을 찾을 수 없습니다.");
        }

        // 5. 완료된 송금 이력이 있는지 확인
        boolean hasCompletedTransfer =
                safeAccountMapper.existsCompletedTransfer(wardId, recipient.getRecipientId());
        if(!hasCompletedTransfer){
            throw new BusinessException(HttpStatus.NOT_FOUND, "RECIPIENT_003", "송금 이력이 있는 수취인을 찾을 수 없습니다.");
        }

        // 6. 이미 안전 계좌로 등록 되어 있는지 확인
        if(Boolean.TRUE.equals(recipient.getIsRegisteredSafe())){
            throw new BusinessException(HttpStatus.CONFLICT, "SAFE_ACCOUNT_001", "이미 등록된 안전계좌입니다.");
        }

        // 7. 신규 등록인지, 재활성화(복구)인지 구분 -> 컨트롤러에서 200/201 분기용
        boolean isFirstRegistration = (recipient.getSafeRegisteredAt() == null);

        // 8. 안전계좌로 등록/복구 (본인 등록이므로 registeredBy = null)
        // 동시 요청 중 하나만 반영
        int updated = safeAccountMapper.registerSafeAccount(recipient.getRecipientId(), null, alias);
        if (updated == 0) {
            throw new BusinessException(HttpStatus.CONFLICT, "SAFE_ACCOUNT_001", "이미 등록된 안전계좌입니다.");
        }

        // 9. 응답
        return SafeAccountResponse.builder()
                .safeAccountId(recipient.getRecipientId())
                .recipientId(recipient.getRecipientId())
                .bankCode(recipient.getBankCode())
                .bankName(bankMapper.findBankName(recipient.getBankCode()))
                .accountNo(recipient.getAccountNo())
                .holderName(recipient.getHolderName())
                .accountAlias(alias)
                .isVerified(true)
                .status("ACTIVE")
                .createdAt(LocalDateTime.now())
                .newlyRegistered(isFirstRegistration)
                .build();
    }

    @Override
    public SafeAccountResponse registerByGuard(Long guardId, Long wardId, GuardSafeAccountRegisterRequest request) {

        // 1. 이 피보호자의 보호자가 이 사람이 맞는지
        if(!guardService.verifyGuardOfWard(guardId, wardId)){
            throw new BusinessException(HttpStatus.NOT_FOUND, "LINK_001", "연동된 피보호자를 찾을 수 없습니다.");
        }

        // 2. 별칭 검증 및 정규화
        String alias = normalizeAlias(request.getAccountAlias());

        // 3. 기존에 등록된 수취인인지 확인
        Recipient recipient = recipientMapper.findRecipient(wardId, request.getBankCode(), request.getAccountNo());

        Long recipientId;
        String holderName;
        String bankName;
        boolean isFirstRegistration;

        if(recipient != null){
        // 이미 존재하는 계좌라면 ?
            if (Boolean.TRUE.equals(recipient.getIsRegisteredSafe())){
                throw new BusinessException(HttpStatus.CONFLICT, "SAFE_ACCOUNT_001", "이미 등록된 안전 계좌 입니다.");
            }
            isFirstRegistration = (recipient.getSafeRegisteredAt() == null);
            // 동시 요청 중 하나만 반영
            int updated = safeAccountMapper.registerSafeAccount(recipient.getRecipientId(), guardId, alias);
            if (updated == 0) {
                throw new BusinessException(HttpStatus.CONFLICT, "SAFE_ACCOUNT_001", "이미 등록된 안전 계좌 입니다.");
            }

            recipientId = recipient.getRecipientId();
            holderName = recipient.getHolderName();
            bankName = bankMapper.findBankName(recipient.getBankCode());
        } else {
            // 처음 등록하는 계좌라면 ? (실명조회 필요)
            RealNameInquiryResponse inquiry = openBankingClient.inquireRealName(
                    request.getBankCode(), request.getAccountNo(), null
            );
            if (!inquiry.isSuccess()){
                throw new BusinessException(HttpStatus.NOT_FOUND, "ACCOUNT_002", "해당 계좌를 찾을 수 없습니다.");
            }

            recipientId = safeAccountMapper.insertSafeAccountByGuard(
                    wardId, request.getBankCode(), request.getAccountNo(),
                    inquiry.getAccountHolderName(), guardId, alias
            );
            holderName = inquiry.getAccountHolderName();
            bankName = inquiry.getBankName();
            isFirstRegistration = true; // 처음 등록하기 때문에 무조건 true
        }

        // 4. 응답
        return SafeAccountResponse.builder()
                .safeAccountId(recipientId)
                .recipientId(recipientId)
                .bankCode(request.getBankCode())
                .bankName(bankName)
                .accountNo(request.getAccountNo())
                .holderName(holderName)
                .accountAlias(alias)
                .isVerified(true)
                .status("ACTIVE")
                .createdAt(LocalDateTime.now())
                .newlyRegistered(isFirstRegistration)
                .build();
    }

    @Override
    public SafeAccountListResponse getSafeAccountListByWard(Long wardId) {
        // 1. 피보호자가 맞는지
        User user = userMapper.findById(wardId);
        if (user.getRole() != Role.WARD){
            throw new BusinessException(HttpStatus.FORBIDDEN, "AUTH_004", "피보호자만 접근할 수 있습니다.");
        }

        // 2. 보호자와 페어링 되어 있는지
        if(!recipientMapper.existsActivePairing(wardId)){
            throw new BusinessException(HttpStatus.FORBIDDEN, "WARD_001", "페어링 완료 후 이용할 수 있습니다.");
        }

        // 3. 조회 (WARD에는 recipientId 노출)
        List<SafeAccountListItem> items = safeAccountMapper.findSafeAccountList(wardId);

        return SafeAccountListResponse.builder()
                .safeAccounts(items)
                .build();

    }

    @Override
    public SafeAccountListResponse getSafeAccountListByGuard(Long guardId, Long wardId) {
        // 1. 이 피보호자의 보호자가 이 사람이 맞는지
        if(!guardService.verifyGuardOfWard(guardId,wardId)){
            throw new BusinessException(HttpStatus.FORBIDDEN, "AUTH_005", "해당 시니어의 보호자가 아닙니다.");
        }

        // 2. 조회
        List<SafeAccountListItem> items = safeAccountMapper.findSafeAccountList(wardId);

        // 3. Guard에선 recipientId X
        items.forEach(item -> item.setRecipientId(null));

        return SafeAccountListResponse.builder()
                .safeAccounts(items)
                .build();
    }

    @Override
    @Transactional
    public SafeAccountDeleteResponse deactivateByWard(Long wardId, Long safeAccountId) {
        // 1. 피보호자인지 확인
        User user = userMapper.findById(wardId);
        if(user.getRole() != Role.WARD){
            throw new BusinessException(HttpStatus.FORBIDDEN, "AUTH_004", "안전계좌를 찾을 수 없습니다.");
        }

        // 2. 내 안전계좌가 맞는지
        Recipient recipient = recipientMapper.findById(safeAccountId);
        if (recipient == null
                || !recipient.getWardId().equals(wardId)
                || recipient.getSafeRegisteredAt() == null){
            throw new BusinessException(HttpStatus.NOT_FOUND, "SAFE_ACCOUNT_003", "안전계좌를 찾을 수 없습니다.");
        }

        // 3. 비활성화, 이미 비활성화 상태라면 영향받은 행 0 => 그래도 정상 응답(멱등)
        safeAccountMapper.deactivateSafeAccount(recipient.getRecipientId());

        // 4. 응답
        return SafeAccountDeleteResponse.builder()
                .safeAccountId(recipient.getRecipientId())
                .status("INACTIVE")
                .build();
    }

    @Override
    @Transactional
    public SafeAccountDeleteResponse deactivateByGuard(Long guardId, Long wardId, Long safeAccountId) {
        // 1. 이 피보호자의 보호자인지
        if(!guardService.verifyGuardOfWard(guardId,wardId)){
            throw new BusinessException(HttpStatus.NOT_FOUND, "LINK_001", "연동된 피보호자를 찾을 수 없습니다.");
        }

        // 2. 대상이 이 피보호자의 안전계좌가 맞는지
        Recipient recipient = recipientMapper.findById(safeAccountId);
        if(recipient == null
                || !recipient.getWardId().equals(wardId)
                || recipient.getSafeRegisteredAt() == null){
            throw new BusinessException(HttpStatus.NOT_FOUND, "SAFE_ACCOUNT_003", "안전계좌를 찾을 수 없습니다.");
        }

        // 3. 비활성화 (멱등)
        safeAccountMapper.deactivateSafeAccount(recipient.getRecipientId());

        // 4. 응답
        return SafeAccountDeleteResponse.builder()
                .safeAccountId(recipient.getRecipientId())
                .status("INACTIVE")
                .build();
    }

    private String normalizeAlias(String alias) {
        if (alias == null || alias.isBlank()) {
            return null;
        }
        if (alias.length() > MAX_ALIAS_LENGTH) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "SAFE_ACCOUNT_004", "입력값이 올바르지 않습니다.");
        }
        return alias;
    }
}
