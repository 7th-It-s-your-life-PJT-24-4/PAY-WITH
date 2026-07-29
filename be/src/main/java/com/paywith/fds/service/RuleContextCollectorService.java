package com.paywith.fds.service;

import com.paywith.fds.dto.FdsEvaluationRequest;
import com.paywith.fds.service.rule.RuleContext;

/**
 * 룰 평가에 필요한 정보를 모으는 책임을 맡는다(0단계 선집계).
 *
 * <p>룰마다 DB를 조회하지 않는 것이 목적이므로, 룰이 늘어도 조회 횟수는 5회로 고정된다.
 * 판정 로직은 갖지 않으며 수집한 값을 {@link RuleContext} 하나로 조립해 넘긴다.
 */
public interface RuleContextCollectorService {

    RuleContext collect(FdsEvaluationRequest request);
}
