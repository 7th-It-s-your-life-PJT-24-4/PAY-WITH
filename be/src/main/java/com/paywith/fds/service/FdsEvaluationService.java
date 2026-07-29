package com.paywith.fds.service;

import com.paywith.fds.dto.FdsDecision;
import com.paywith.fds.dto.FdsEvaluationRequest;

/**
 * 송금 FDS 평가 진입점.
 *
 * <p>판정 흐름은 4단계다.
 * <ol>
 *   <li>컨텍스트 선집계 — 룰 지표를 DB에서 한 번에 모은다.</li>
 *   <li>단축평가 — 블랙리스트(즉시 DANGER) → 화이트리스트(즉시 SAFE) 순으로 확정 판정을 시도한다.</li>
 *   <li>룰 판정 — 캐시된 활성 룰을 순회해 점수를 합산한다.</li>
 *   <li>등급 판정 — 경계값 두 개로 SAFE/CAUTION/DANGER 를 가른다.</li>
 * </ol>
 *
 * <p>송금 API 쪽 사용 계약:
 * <ol>
 *   <li>수취인을 먼저 확보한다(recipients 조회, 없으면 insert). 신규 여부는 행 존재가 아니라
 *       send_count=0 으로 판정하므로 방금 등록한 수취인도 정상 평가된다.</li>
 *   <li>거래 행을 status=REQUESTED 로 먼저 생성한다. risk_evaluations 가 transaction_id 를
 *       NOT NULL FK 로 참조하므로, 블랙리스트 즉시 차단도 거래 행이 있어야 근거를 남길 수 있다.</li>
 *   <li>{@link #evaluate(FdsEvaluationRequest)} 로 판정을 받는다. 트랜잭션 밖에서 호출한다.</li>
 *   <li>{@link FdsDecision#getRiskLevel()} 로 거래 상태를 정한다.
 *       SAFE/CAUTION 은 PROCESSING, DANGER 는 HELD, 블랙리스트 차단은 BLOCKED.</li>
 *   <li>{@link #saveDecision(Long, FdsDecision)} 를 호출한다. 평가 저장·risk_score 갱신·승인요청 생성이
 *       하나의 트랜잭션으로 묶인다.</li>
 * </ol>
 */
public interface FdsEvaluationService {

    /** 송금 요청을 평가한다. DB 조회만 하며 쓰기는 하지 않는다. */
    FdsDecision evaluate(FdsEvaluationRequest request);

    /** 판정 결과를 저장한다. 위험(DANGER) 판정이면 보호자 승인요청 생성까지 함께 처리한다. */
    void saveDecision(Long transactionId, FdsDecision decision);
}
