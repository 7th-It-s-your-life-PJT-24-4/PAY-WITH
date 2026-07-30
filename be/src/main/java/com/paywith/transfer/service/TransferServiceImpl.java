package com.paywith.transfer.service;

import com.paywith.exception.BusinessException;
import com.paywith.external.openbanking.OpenBankingClient;
import com.paywith.external.openbanking.dto.RealNameInquiryResponse;
import com.paywith.recipient.domain.Recipient;
import com.paywith.recipient.mapper.RecipientMapper;
import com.paywith.transaction.domain.Transaction;
import com.paywith.transaction.mapper.TransactionMapper;
import com.paywith.transfer.dto.RecipientInquiryRequest;
import com.paywith.transfer.dto.RecipientInquiryResponse;
import com.paywith.transfer.dto.TransferRequest;
import com.paywith.transfer.dto.TransferResponse;
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
public class TransferServiceImpl implements TransferService{

    private final OpenBankingClient openBankingClient;
    private final WalletMapper walletMapper;
    private final PasswordEncoder passwordEncoder;
    private final RecipientMapper recipientMapper;
    private final TransactionMapper transactionMapper;
    private final UserMapper userMapper;

    @Override
    public RecipientInquiryResponse inquireRecipient(RecipientInquiryRequest request) {

        RealNameInquiryResponse inquiryResponse = openBankingClient.inquireRealName(
                request.getBankCode(),
                request.getAccountNo(),
                null
        );

        if(!inquiryResponse.isSuccess()){
            throw new BusinessException(HttpStatus.NOT_FOUND, "해당 계좌를 찾을 수 없습니다.");
        }

        return RecipientInquiryResponse.builder()
                .bankCode(request.getBankCode())
                .bankName(inquiryResponse.getBankName())
                .accountNo(request.getAccountNo())
                .recipientName(inquiryResponse.getAccountHolderName())
                .build();
    }

    @Override
    @Transactional
    public TransferResponse transfer(Long userId, TransferRequest request) {
        //1. 지갑을 조회한다
        Wallet wallet = walletMapper.findWalletByUserId(userId);
        if(wallet==null){
            throw new BusinessException(HttpStatus.BAD_REQUEST, "지갑을 찾을 수 없습니다");
        }

        //2. transferPin 검증
        User user = userMapper.findById(userId);
        if(!passwordEncoder.matches(request.getTransferPin(),wallet.getPin())){ //TODO: users.getPin으로 바꾸기
            throw new BusinessException(HttpStatus.BAD_REQUEST, "송금 비밀번호가 올바르지 않습니다.");
        }

        //3. 수취인 실명조회
        RealNameInquiryResponse inquiryResponse = openBankingClient.inquireRealName(
                request.getBankCode(),
                request.getAccountNo(),
                null
        );
        if (!inquiryResponse.isSuccess()){
            throw new BusinessException(HttpStatus.NOT_FOUND, "해당계좌를 찾을 수 없습니다.");
        }

        //4. 수취인 조회/등록
        Recipient recipient = recipientMapper.findRecipient(userId, request.getBankCode(), request.getAccountNo());
        if(recipient == null){
            recipient = Recipient.builder()
                    .wardId(userId)
                    .bankCode(request.getBankCode())
                    .accountNo(request.getAccountNo())
                    .holderName(inquiryResponse.getAccountHolderName())
                    .build();
            recipientMapper.insertRecipient(recipient);
        }
        else {
            recipientMapper.updateSendInfo(recipient.getRecipientId());
        }


        //5. 잔액 조건부 차감
        int affectedRows = walletMapper.decreaseBalanceIfSufficient(wallet.getWalletId(), request.getAmount());
        if (affectedRows == 0){
            throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, "송금 가능한 잔액이 부족합니다.");
        }


        //6. 입금이체
        openBankingClient.deposit(request.getBankCode(), request.getAccountNo(), request.getAmount());


        // 7. 거래 기록 생성
        Long balanceAfter = wallet.getBalance() - request.getAmount();

        Transaction transaction = Transaction.builder()
                .walletId(wallet.getWalletId())
                .recipientId(recipient.getRecipientId())
                .type("TRANSFER_OUT")
                .initiatedBy(null)
                .amount(request.getAmount())
                .memo(request.getMemo())
                .balanceAfter(balanceAfter)
                .status("COMPLETED")
                .createdAt(LocalDateTime.now())
                .completedAt(LocalDateTime.now())
                .build();

        transactionMapper.insertTransaction(transaction);

        // 8. 응답 조립
        return TransferResponse.builder()
                .transactionId(transaction.getTransactionId())
                .status(transaction.getStatus())
                .holderName(inquiryResponse.getAccountHolderName())
                .bankCode(request.getBankCode())
                .bankName(inquiryResponse.getBankName())
                .accountNo(request.getAccountNo())
                .amount(request.getAmount())
                .memo(request.getMemo())
                .completedAt(transaction.getCompletedAt())
                .balanceAfter(balanceAfter)
                .build();
    }

}
