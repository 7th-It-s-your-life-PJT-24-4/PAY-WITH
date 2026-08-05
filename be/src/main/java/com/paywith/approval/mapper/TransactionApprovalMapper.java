package com.paywith.approval.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * transactions.status 의 승인 결과(APPROVED/REJECTED) 반영 전용.
 *
 * <p>이 값은 approval_requests.status 의 비정규화 복사본이라 승인 처리 서비스가 두 테이블을
 * 같은 트랜잭션에서 함께 갱신해야 한다(schema.sql 의 single writer 규칙). 다른 경로에서
 * 단독으로 갱신하지 않도록 매퍼를 승인 패키지에 두고 용도를 이름으로 못박는다.
 */
@Mapper
public interface TransactionApprovalMapper {

    /**
     * 상태 조건을 걸지 않는다. 승인/거절 권한은 approval_requests 의 조건부 UPDATE 가 이미
     * 판정했고(승자 한 명), approval_requests.transaction_id 는 UNIQUE + FK 이므로 대상 행은
     * 반드시 하나 존재한다.
     */
    int updateStatus(@Param("transactionId") Long transactionId, @Param("status") String status);

    /**
     * 승인 직후 이어진 송금이 입금 호출 전에 실패했을 때 거래를 FAILED 로 종결한다.
     *
     * <p>{@link #updateStatus} 와 달리 승인 트랜잭션 밖에서(커밋 이후) 호출되므로 상태 조건을
     * 건다. APPROVED 인 행만 바꾸기 때문에, 같은 거래가 이미 PROCESSING/COMPLETED 로 넘어갔다면
     * 0 을 돌려주고 아무것도 덮어쓰지 않는다.
     *
     * <p>입금 호출 이후의 실패는 여기서 다루지 않는다. 그쪽은 잔액이 이미 차감돼 수동 정산이
     * 필요한 별개 상황이고, TransferFinalizationServiceImpl 이 자체적으로 FAILED 를 기록한다.
     *
     * @return 영향 행 수. 0 이면 이미 다른 상태로 진행된 거래다.
     */
    int markFailedIfApproved(@Param("transactionId") Long transactionId);

    /**
     * 응답 시한이 지난 승인 대기 건의 거래를 CANCELED 로 돌린다.
     *
     * <p>schema.sql 의 approval_requests.status 주석이 정한 대로 EXPIRED 의 짝은 CANCELED 다.
     * 보류 시점에는 잔액을 잡아두지 않으므로(차감은 승인 후 송금 실행에서만 일어난다) 되돌릴
     * 잔액은 없고 상태만 종결하면 된다.
     *
     * <p>{@code ApprovalRequestMapper.expireOverdue} 보다 <b>먼저</b> 실행해야 한다. 조인 조건이
     * 아직 PENDING 인 승인요청을 찾기 때문이다. HELD 인 거래만 바꾸므로, 어떤 이유로 거래가
     * 이미 다른 상태로 넘어갔다면 건드리지 않는다.
     *
     * @return CANCELED 로 바꾼 거래 수
     */
    int cancelHeldForExpiredApprovals();

    /** 피보호자가 직접 취소한 승인 대기 거래를 HELD 에서 CANCELED 로 종결한다. */
    int cancelHeldByWard(@Param("transactionId") Long transactionId);
}
