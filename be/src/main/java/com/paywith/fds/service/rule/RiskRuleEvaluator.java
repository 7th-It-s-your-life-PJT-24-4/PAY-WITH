package com.paywith.fds.service.rule;

public interface RiskRuleEvaluator {

    String getRuleCode();

    boolean evaluate(RuleContext context);
}
