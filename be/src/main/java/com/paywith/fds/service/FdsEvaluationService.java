package com.paywith.fds.service;

import com.paywith.fds.dto.FdsDecision;
import com.paywith.fds.dto.FdsEvaluationRequest;

/**
 * 송금 FDS 평가 진입점. 수집 → 단축평가 → 룰 점수 → 등급 순으로 판정하고 결과 저장까지 끝낸다.
 *
 * <p>호출 순서:
 * <ol>
 *   <li>수취인 확보(recipients 조회, 없으면 insert)</li>
 *   <li>거래 행을 status=REQUESTED 로 생성 — risk_evaluations 가 transaction_id 를 FK 로 참조한다</li>
 *   <li>{@link #evaluate(FdsEvaluationRequest)} 호출</li>
 *   <li>반환 판정으로 상태 결정 — SAFE/CAUTION 은 PROCESSING, 블랙리스트는 BLOCKED,
 *       나머지 DANGER 는 HELD</li>
 * </ol>
 */
public interface FdsEvaluationService {

    /**
     * 송금을 평가하고 결과를 저장한 뒤 판정을 반환한다.
     *
     * <p>등급만으로는 같은 DANGER 안에서 즉시 차단과 승인 대기를 구분할 수 없어 판정 전체를
     * 돌려준다. 호출부는 {@code isBlocked()} / {@code isHeld()} 로 분기한다.
     *
     * <p>내부에 외부 API 조회가 있으므로 트랜잭션 밖에서 호출해야 한다. 저장은 별도 트랜잭션에서
     * 커밋되므로, 이 호출 뒤 거래 상태 갱신이 실패하면 평가 기록과 상태가 어긋난다.
     */
    FdsDecision evaluate(FdsEvaluationRequest request);
}
