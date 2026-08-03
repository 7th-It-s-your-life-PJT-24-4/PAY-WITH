package com.paywith.approval.mapper;

import com.paywith.approval.domain.ApprovalRequest;
import com.paywith.approval.domain.ApprovalRequestView;
import com.paywith.approval.dto.ApprovalRuleHitResponse;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ApprovalRequestMapper {

    int insert(ApprovalRequest approvalRequest);

    /**
     * 보호자가 담당하는 시니어들의 승인 대기 목록.
     *
     * <p>guard_senior 를 ACTIVE 로 조인하므로 담당이 아닌 시니어의 건은 애초에 조회되지 않는다.
     * 만료 스캔이 status 를 EXPIRED 로 바꾸기 전까지의 공백이 있어 expired_at 으로도 거른다.
     *
     * @param wardId null 이면 담당 전체, 값이 있으면 그 피보호자 건만. 담당이 아닌 값을 주면
     *                 담당 조인에서 걸려 빈 목록이 된다.
     */
    List<ApprovalRequestView> findPendingByGuardId(
        @Param("guardId") Long guardId,
        @Param("wardId") Long wardId);

    /**
     * 승인 대기 건의 상세. 목록과 같은 조건(대기 중 + 미만료)을 걸어 두 조회가 "대기 중"을 같은
     * 의미로 쓴다. 담당 관계가 없어도 null 이 되어 권한 확인을 겸한다.
     */
    ApprovalRequestView findByIdAndGuardId(
        @Param("approvalId") Long approvalId,
        @Param("guardId") Long guardId);

    /**
     * 승인/거절 처리에서만 쓰는 내부 조회. 화면에 나가지 않는다.
     *
     * <p>approval_requests 에는 guard_id 가 없어 거래→지갑→시니어→담당관계를 타고 올라가야
     * 담당 여부를 알 수 있다. 그 김에 갱신 대상 transaction_id 까지 같은 조회에서 얻는다.
     *
     * <p>상태와 만료는 일부러 보지 않는다. 그 판정은 {@link #updateDecision} 이 하며, 여기서
     * 미리 걸러내면 "이미 처리됐거나 만료됨"(409)이 "찾을 수 없음"(404)으로 뭉개진다.
     *
     * @return 담당 시니어의 승인요청이면 대상 거래 ID, 아니면 null
     */
    Long findTransactionIdByIdAndGuardId(
        @Param("approvalId") Long approvalId,
        @Param("guardId") Long guardId);

    /**
     * 보류 사유. 점수가 큰 순으로 정렬해 보호자가 주된 근거부터 보게 한다.
     *
     * <p>FDS 테이블(risk_evaluations 계열)을 읽는다. 위 조회들이 이미 risk_evaluations 를
     * 조인하고 있어 접근 경로를 하나로 맞춘 것이다.
     */
    List<ApprovalRuleHitResponse> findRuleHits(@Param("transactionId") Long transactionId);

    /**
     * 승인/거절 확정. 대기 상태이고 아직 만료되지 않은 행만 바꾼다.
     *
     * <p>조건을 WHERE 에 넣어 상태 검사와 갱신을 한 문장으로 처리하므로, 보호자의 승인 클릭이
     * 만료 처리나 다른 보호자의 동시 처리와 겹쳐도 한쪽만 성공한다. 만료 배치가 없어도
     * expired_at 조건 덕분에 시간이 지난 요청은 승인될 수 없다.
     *
     * @return 영향 행 수. 0 이면 이미 처리됐거나 만료된 요청이다.
     */
    int updateDecision(
        @Param("approvalId") Long approvalId,
        @Param("guardId") Long guardId,
        @Param("status") String status,
        @Param("respondedAt") LocalDateTime respondedAt);

    /**
     * 응답 시한이 지난 승인 대기 건을 EXPIRED 로 종결한다.
     *
     * <p>이 갱신 전에 {@code TransactionApprovalMapper.cancelHeldForExpiredApprovals} 로 대상
     * 거래를 먼저 CANCELED 로 바꿔야 한다. 여기서 status 를 먼저 바꾸면 거래 쪽 조인 조건
     * (PENDING)이 더는 맞지 않아 거래가 HELD 로 남는다.
     *
     * <p>{@link #updateDecision} 이 {@code expired_at > NOW()} 를 요구하므로, 보호자의 승인과
     * 이 만료 처리가 겹쳐도 한쪽만 성공한다. 둘 다 PENDING 인 행만 바꾸기 때문이다.
     *
     * @return 만료 처리한 승인요청 수
     */
    int expireOverdue();
}
