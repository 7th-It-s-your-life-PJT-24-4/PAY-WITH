package com.paywith.approval.service;

import com.paywith.approval.mapper.ApprovalRequestMapper;
import com.paywith.approval.mapper.TransactionApprovalMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ApprovalExpiryServiceImpl implements ApprovalExpiryService {

    private static final Logger log = LoggerFactory.getLogger(ApprovalExpiryServiceImpl.class);

    private final ApprovalRequestMapper approvalRequestMapper;
    private final TransactionApprovalMapper transactionApprovalMapper;

    /**
     * 거래를 먼저 CANCELED 로 바꾸고 그 다음 승인요청을 EXPIRED 로 바꾼다. 순서가 뒤바뀌면
     * 거래 쪽 조인이 PENDING 인 승인요청을 찾지 못해 거래가 HELD 로 남는다.
     *
     * <p>두 갱신을 한 트랜잭션으로 묶어, 중간에 실패해도 승인요청만 만료되고 거래는 보류로
     * 남는 어긋난 상태가 생기지 않게 한다.
     *
     * <p>양쪽 모두 조건부 UPDATE 라 보호자의 승인·거절과 겹쳐도 한쪽만 성공한다. 별도의 잠금은
     * 필요 없고, 앱 인스턴스가 여럿이어도 같은 건을 두 번 종결하지 않는다.
     */
    @Override
    @Transactional
    public int expireOverdue() {
        int canceled = transactionApprovalMapper.cancelHeldForExpiredApprovals();
        int expired = approvalRequestMapper.expireOverdue();

        if (expired > 0) {
            log.info("승인요청 만료 처리 {}건 (거래 취소 {}건)", expired, canceled);
        }
        if (expired != canceled) {
            // 정상 흐름에서는 두 값이 같다. 어긋나면 보류 거래와 승인요청의 상태가 따로 논다는
            // 뜻이므로(HELD 가 아닌 거래에 PENDING 승인요청이 달려 있는 등) 확인이 필요하다.
            log.warn("만료 처리 건수 불일치. 승인요청={}, 거래={}", expired, canceled);
        }
        return expired;
    }
}
