package com.paywith.fds.service.prefilter;

import com.paywith.fds.domain.DecidedBy;
import com.paywith.fds.domain.RiskLevel;
import com.paywith.fds.service.rule.RuleContext;
import java.util.Optional;

/**
 * 단축평가(블랙/화이트리스트). 확정적 판정만 담당하며, 확률적 점수 계산은 룰 단계가 맡는다.
 *
 * <p>판정이 서면 룰 단계를 건너뛰고 등급이 즉시 확정된다. 흐름을 예외로 끊지 않고 Optional 로
 * 표현하는 이유는, 정상 판정 경로가 예외 핸들러로 흩어지면 추적이 어려워지기 때문이다.
 *
 * <p>구현체는 {@link org.springframework.core.annotation.Order} 로 순서를 지정한다.
 * 블랙리스트가 화이트리스트보다 먼저 평가되어야 한다. 보호자가 등록한 안전계좌라도
 * 차단 사유가 확인되면 막아야 하기 때문이다.
 */
public interface FdsPreFilter {

    /** risk_rules 에 등록된 rule_code. 발동 시 risk_evaluation_details 에 근거로 남는다. */
    String getRuleCode();

    /** 이 필터가 확정하는 판정 경로. */
    DecidedBy getDecidedBy();

    /** 판정이 서면 해당 등급, 아니면 비어 있는 Optional(다음 단계로 진행). */
    Optional<RiskLevel> apply(RuleContext context);
}
