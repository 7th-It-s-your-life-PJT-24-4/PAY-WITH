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
}
