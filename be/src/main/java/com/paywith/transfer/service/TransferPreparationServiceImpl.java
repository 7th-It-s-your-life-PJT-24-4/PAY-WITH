package com.paywith.transfer.service;

import com.paywith.exception.BusinessException;
import com.paywith.external.openbanking.OpenBankingClient;
import com.paywith.external.openbanking.dto.RealNameInquiryResponse;
import com.paywith.recipient.domain.Recipient;
import com.paywith.recipient.mapper.RecipientMapper;
import com.paywith.transaction.domain.Transaction;
import com.paywith.transaction.mapper.TransactionMapper;
import com.paywith.transfer.dto.PreparedTransfer;
import com.paywith.transfer.dto.TransferRequest;
import com.paywith.user.domain.User;
import com.paywith.user.mapper.UserMapper;
import com.paywith.wallet.domain.Wallet;
import com.paywith.wallet.mapper.WalletMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class TransferPreparationServiceImpl implements TransferPreparationService {

    private final WalletMapper walletMapper;
    private final PasswordEncoder passwordEncoder;
    private final OpenBankingClient openBankingClient;
    private final RecipientMapper recipientMapper;
    private final TransactionMapper transactionMapper;
    private final UserMapper userMapper;

    @Override
    @Transactional
    public PreparedTransfer prepare(Long userId, TransferRequest request) {
        //1. 지갑을 조회한다
        Wallet wallet = walletMapper.findWalletByUserId(userId);
        if (wallet == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "지갑을 찾을 수 없습니다");
        }

        //2. transferPin 검증
        User user = userMapper.findById(userId);
        if (!passwordEncoder.matches(request.getTransferPin(), user.getPin())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "송금 비밀번호가 올바르지 않습니다.");
        }


        //3. 수취인 실명조회
        RealNameInquiryResponse inquiryResponse = openBankingClient.inquireRealName(
                request.getBankCode(),
                request.getAccountNo(),
                null
        );
        if (!inquiryResponse.isSuccess()) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "해당계좌를 찾을 수 없습니다.");
        }

        //4. 수취인 조회/등록
        Recipient recipient = recipientMapper.findRecipient(userId, request.getBankCode(), request.getAccountNo());
        if (recipient == null) {
            recipient = Recipient.builder()
                    .wardId(userId)
                    .bankCode(request.getBankCode())
                    .accountNo(request.getAccountNo())
                    .holderName(inquiryResponse.getAccountHolderName())
                    .build();
            recipientMapper.insertRecipient(recipient);
        } else {
            recipientMapper.updateSendInfo(recipient.getRecipientId());
        }

        // 5. fds 전에 거래 행을 REQUESTED로 먼저 생성 (risk_evaluation -> transaction_id를 FK로 참조
        Transaction transaction = Transaction.builder()
                .walletId(wallet.getWalletId())
                .recipientId(recipient.getRecipientId())
                .type("TRANSFER_OUT")
                .initiatedBy(null)
                .amount(request.getAmount())
                .memo(request.getMemo())
                .status("REQUESTED")
                .createdAt(LocalDateTime.now())
                .build();
        transactionMapper.insertTransaction(transaction);


        return PreparedTransfer.builder()
                .wallet(wallet)
                .recipient(recipient)
                .transaction(transaction)
                .inquiryResponse(inquiryResponse)
                .build();
    }
}