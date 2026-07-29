package com.paywith.fds.service;

import com.paywith.fds.dto.FdsEvaluationRequest;
import com.paywith.fds.service.rule.RuleContext;

/** 판정에 필요한 정보를 모은다. 룰이 늘어도 조회 횟수가 늘지 않게 하는 것이 목적이다. */
public interface RuleContextCollectorService {

    RuleContext collect(FdsEvaluationRequest request);
}
