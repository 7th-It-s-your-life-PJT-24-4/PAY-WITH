package com.paywith.transaction.service;

import com.paywith.exception.BusinessException;
import com.paywith.guard.service.GuardService;
import com.paywith.transaction.dto.*;
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

    private static final Set<String> ALLOWED_TYPES = Set.of("CHARGE", "TRANSFER", "PAYMENT");
    private static final Set<String> ALLOWED_RISK_LEVELS = Set.of("SAFE", "CAUTION", "DANGER");

    private final TransactionMapper transactionMapper;
    private final WalletService walletService;
    private final GuardService guardService;

    @Override
    @Transactional(readOnly = true)
    public TransactionHistoryListResponse findMyTransactions(Long userId, String category, String keyword, Integer page, Integer size) {

        // 1. 신원 확인, 권한 확인 => 지갑이 있는 지 없는지
        walletService.findMyBalance(userId);

        // 2. category 안에 있는지 확인
        String resolvedCategory = (category == null || category.isBlank()) ? "ALL" : category;
        if(!ALLOWED_CATEGORIES.contains(resolvedCategory)){
            throw new BusinessException(HttpStatus.BAD_REQUEST, "TRANSACTION_001", "조회 조건이 올바르지 않습니다.");
        }

        // 3. page값이 음수거나, size 값이 0 이하거나 100 초과하면 걸러냄
        int resolvedPage = (page == null) ? DEFAULT_PAGE : page;
        int resolvedSize = (size == null) ? DEFAULT_SIZE : size;
        if(resolvedPage < 0 || resolvedSize < 1 || resolvedSize > MAX_SIZE){
            throw new BusinessException(HttpStatus.BAD_REQUEST,"TRANSACTION_001", "조회 조건이 올바르지 않습니다.");
        }

        // 4. API 표현을 DB 표현으로 변경
        String type = switch (resolvedCategory) {
            case "ALL" -> null;
            case "TRANSFER" -> "TRANSFER_OUT";
            default -> resolvedCategory; // CHARGE, PAYMENT는 DB 값과 동일
        };

        // 5. 페이지 번호를 SQL의 OFFSET으로 변환
        int offset = resolvedPage * resolvedSize;

        // 6. 실제 조회
        List<TransactionHistoryItem> items =
                transactionMapper.findMyTransactions(userId, type, keyword, offset, resolvedSize);
        int totalElements = transactionMapper.countMyTransactions(userId, type, keyword);

        // 7. 응답
        return new TransactionHistoryListResponse(items, resolvedPage, resolvedSize, totalElements);
    }

    @Override
    @Transactional(readOnly = true)
    public GuardTransactionHistoryListResponse findWardTransactions(Long guardId, Long wardId, String type, String riskLevel, Integer page, Integer size) {

        // 1. 보호자와 피보호자가 연동 되어 있는지
        if (!guardService.verifyGuardOfWard(guardId, wardId)){
            throw new BusinessException(HttpStatus.NOT_FOUND, "LINK_001", "연동된 피보호자를 찾을 수 없습니다.");
        }

        // 2. type과 riskLevel이 null이 아니고 ALLOWED_TYPES, ALLOWED_RISK_LEVELS 안에 있는지 확인
        if (type != null && !ALLOWED_TYPES.contains(type)){
            throw new BusinessException(HttpStatus.BAD_REQUEST, "REQUEST_001", "요청 값이 올바르지 않습니다.");
        }
        if (riskLevel != null && !ALLOWED_RISK_LEVELS.contains(riskLevel)){
            throw new BusinessException(HttpStatus.BAD_REQUEST, "REQUEST_001", "요청 값이 올바르지 않습니다.");
        }

        // 3. page값이 음수거나, size 값이 0 이하거나 100 초과하면 걸러냄
        int resolvedPage = (page == null) ? DEFAULT_PAGE : page;
        int resolvedSize = (size == null) ? DEFAULT_SIZE : size;
        if (resolvedPage < 0 || resolvedSize < 1 || resolvedSize > MAX_SIZE){
            throw new BusinessException(HttpStatus.BAD_REQUEST, "REQUEST_001", "요청 값이 올바르지 않습니다.");
        }

        // 4. API 표현을 DB 표현으로 변경
        String dbType = "TRANSFER".equals(type) ? "TRANSFER_OUT" : type;

        // 5. 페이지 번호를 SQL의 OFFSET으로 변환
        int offset = resolvedPage * resolvedSize;

        // 6. 실제 조회
        List<GuardTransactionHistoryItem> items =
                transactionMapper.findWardTransactions(guardId, wardId, dbType, riskLevel, offset, resolvedSize);
        int totalElements =
                transactionMapper.countWardTransactions(guardId,wardId, dbType, riskLevel);

        // 7. 응답
        return new GuardTransactionHistoryListResponse(items, resolvedPage, resolvedSize,totalElements);
    }

    @Override
    @Transactional(readOnly = true)
    public TransactionDetailResponse findMyTransactionDetail(Long userId, Long transactionId) {
        // 1. 조회
        TransactionDetailResponse detail = transactionMapper.findMyTransactionDetail(transactionId, userId);
        if (detail == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "TRANSACTION_003", "거래 내역을 찾을 수 없습니다.");
        }

        // 2. riskAnalysis 조립 후 필드 정리
        detail.setRiskAnalysis(
                buildRiskAnalysis(transactionId, detail.getType(), detail.getRiskLevel(), detail.getRiskScore())
        );
        detail.setRiskScore(null);

        return detail;
    }

    @Override
    @Transactional(readOnly = true)
    public GuardTransactionDetailResponse findWardTransactionDetail(Long guardId, Long wardId, Long transactionId) {
        // 1. 보호자-피보호자 연동 확인
        if(!guardService.verifyGuardOfWard(guardId,wardId)){
            throw new BusinessException(HttpStatus.NOT_FOUND, "LINK_001", "연동된 피보호자를 찾을 수 없습니다.");
        }

        // 2. 조회
        GuardTransactionDetailResponse detail = transactionMapper.findWardTransactionDetail(transactionId, wardId);
        if (detail == null){
            throw new BusinessException(HttpStatus.NOT_FOUND, "TRANSACTION_003", "거래 내역을 찾을 수 없습니다.");
        }

        // 3. riskAnalysis 조립 후 필드 정리
        detail.setRiskAnalysis(
                buildRiskAnalysis(transactionId, detail.getType(), detail.getRiskLevel(), detail.getRiskScore())
        );
        detail.setRiskScore(null);

        return  detail;
    }

    // riskAnalysis 조립
    private RiskAnalysisResponse buildRiskAnalysis(Long transactionId, String type, String riskLevel, Integer riskScore) {
        if (!"TRANSFER".equals(type) && !"PAYMENT".equals(type)) {
            return null; // CHARGE 평가 대상이 아님
        }
        if (riskLevel == null || "SAFE".equals(riskLevel) || riskScore == null) {
            return null; // 평가 대상 아니었거나, 안전 거래
        }

        List<String> reasons = transactionMapper.findRiskReasons(transactionId);
        String summary = transactionMapper.findLlmSummary(transactionId);
        return new RiskAnalysisResponse(riskScore, summary, reasons);
    }

}
