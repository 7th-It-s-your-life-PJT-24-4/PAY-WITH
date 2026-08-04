package com.paywith.home.service;

import com.paywith.approval.mapper.ApprovalRequestMapper;
import com.paywith.exception.BusinessException;
import com.paywith.home.dto.PendingApprovalItemResponse;
import com.paywith.home.dto.WardHomeResponse;
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
 * <p>여러 도메인의 조회를 모으기만 하고 자체 규칙을 두지 않는다. 역할 검증 같은 판단은
 * 소유 도메인(여기서는 {@link WalletService})에 남겨, 홈이 규칙을 복제하지 않게 한다.
 * 홈에서만 필요한 규칙이 생기면 여기 넣지 말고 해당 도메인으로 내린다.
 */
@Service
@RequiredArgsConstructor
public class WardHomeServiceImpl implements WardHomeService {

    private final UserMapper userMapper;
    private final WalletService walletService;
    private final ApprovalRequestMapper approvalRequestMapper;

    /**
     * 조회만 하므로 {@code readOnly} 로 둔다. 세 조회를 한 트랜잭션으로 묶어 잔액과 승인 목록이
     * 서로 다른 시점을 보지 않게 한다. 승인 만료 배치가 도는 중에도 화면이 일관되게 나온다.
     *
     * <p>{@code WARD} 검증과 지갑 조회는 {@link WalletService#findMyBalance} 에 맡긴다. 사용자
     * 행을 여기서 한 번, 그 안에서 또 한 번 읽지만 둘 다 PK 조회이고, 그 비용보다 역할 검증 정책이
     * 지갑 도메인 한 곳에만 있는 편이 낫다.
     */
    @Override
    @Transactional(readOnly = true)
    public WardHomeResponse findMyHome(Long userId) {
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다.");
        }

        WalletBalanceResponse wallet = walletService.findMyBalance(userId);

        List<PendingApprovalItemResponse> pendingApprovals =
            approvalRequestMapper.findPendingByWardId(userId).stream()
                .map(PendingApprovalItemResponse::new)
                .collect(Collectors.toList());

        return new WardHomeResponse(user.getName(), wallet, pendingApprovals);
    }
}
