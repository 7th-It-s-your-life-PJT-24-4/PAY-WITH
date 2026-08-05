package com.paywith.home.service;

import com.paywith.approval.mapper.ApprovalRequestMapper;
import com.paywith.exception.BusinessException;
import com.paywith.guard.mapper.GuardSeniorMapper;
import com.paywith.home.dto.PendingApprovalItemResponse;
import com.paywith.home.dto.WardHomeResponse;
import com.paywith.user.domain.Role;
import com.paywith.user.domain.User;
import com.paywith.user.mapper.UserMapper;
import com.paywith.wallet.dto.WalletBalanceResponse;
import com.paywith.wallet.service.WalletService;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 홈 화면 조합 전용 서비스.
 *
 * <p>여러 도메인의 조회를 조합하고, 홈 API 명세가 요구하는 역할 및 페어링 선행 조건을 확인한다.
 * 지갑 자체의 조회 규칙은 {@link WalletService}에 남긴다.
 */
@Service
@RequiredArgsConstructor
public class WardHomeServiceImpl implements WardHomeService {

    private final UserMapper userMapper;
    private final WalletService walletService;
    private final ApprovalRequestMapper approvalRequestMapper;
    private final GuardSeniorMapper guardSeniorMapper;

    /**
     * 조회만 하므로 {@code readOnly} 로 둔다. 조회들을 한 트랜잭션으로 묶어 잔액과 승인 목록이
     * 서로 다른 시점을 보지 않게 한다. 승인 만료 배치가 도는 중에도 화면이 일관되게 나온다.
     *
     * <p>역할과 ACTIVE 페어링 여부는 지갑보다 먼저 확인해 명세의 {@code AUTH_004},
     * {@code WARD_001}, {@code WALLET_001} 순서로 실패 조건이 서로 가려지지 않게 한다.
     */
    @Override
    @Transactional(readOnly = true)
    public WardHomeResponse findMyHome(Long userId) {
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다.");
        }
        if (user.getRole() != Role.WARD) {
            throw new BusinessException(
                HttpStatus.FORBIDDEN,
                "AUTH_004",
                "피보호자만 접근할 수 있습니다."
            );
        }

        if (!guardSeniorMapper.existsActiveRelationByWardId(userId)) {
            throw new BusinessException(
                HttpStatus.FORBIDDEN,
                "WARD_001",
                "페어링 완료 후 이용할 수 있습니다."
            );
        }

        WalletBalanceResponse wallet = walletService.findMyBalance(userId);

        List<PendingApprovalItemResponse> pendingApprovals =
            approvalRequestMapper.findPendingByWardId(userId).stream()
                .map(PendingApprovalItemResponse::new)
                .collect(Collectors.toList());

        return new WardHomeResponse(user.getName(), wallet, pendingApprovals);
    }
}
