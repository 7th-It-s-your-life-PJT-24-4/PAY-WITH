package com.paywith.transaction.service;

import com.paywith.exception.BusinessException;
import com.paywith.transaction.dto.TransactionHistoryItem;
import com.paywith.transaction.dto.TransactionHistoryListResponse;
import com.paywith.transaction.mapper.TransactionMapper;
import com.paywith.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;


@Service
@RequiredArgsConstructor
public class TransactionHistoryServiceImpl implements TransactionHistoryService {

    private static final Set<String> ALLOWED_CATEGORIES = Set.of("ALL", "CHARGE", "TRANSFER", "PAYMENT");
    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private final TransactionMapper transactionMapper;
    private final WalletService walletService;

    @Override
    @Transactional(readOnly = true)
    public TransactionHistoryListResponse findMyTransactions(Long userId, String category, String keyword, Integer page, Integer size) {
        walletService.findMyBalance(userId);

        String resolvedCategory = (category == null || category.isBlank()) ? "ALL" : category;
        if(!ALLOWED_CATEGORIES.contains(resolvedCategory)){
            throw new BusinessException(HttpStatus.BAD_REQUEST, "TRANSACTION_001", "조회 조건이 올바르지 않습니다.");
        }

        int resolvedPage = (page == null) ? DEFAULT_PAGE : page;
        int resolvedSize = (size == null) ? DEFAULT_SIZE : size;
        if(resolvedPage < 0 || resolvedSize < 1 || resolvedSize > MAX_SIZE){
            throw new BusinessException(HttpStatus.BAD_REQUEST,"TRANSACTION_001", "조회 조건이 올바르지 않습니다.");
        }

        String type = switch (resolvedCategory) {
            case "ALL" -> null;
            case "TRANSFER" -> "TRANSFER_OUT";
            default -> resolvedCategory; // CHARGE, PAYMENT는 DB 값과 동일
        };

        int offset = resolvedPage * resolvedSize;

        List<TransactionHistoryItem> items =
                transactionMapper.findMyTransactions(userId, type, keyword, offset, resolvedSize);
        int totalElements = transactionMapper.countMyTransactions(userId, type, keyword);

        return new TransactionHistoryListResponse(items, resolvedPage, resolvedSize, totalElements);
    }

}
