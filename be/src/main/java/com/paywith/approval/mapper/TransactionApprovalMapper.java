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
}
