package com.paywith.payment.fds.support;

import com.paywith.fds.domain.RiskRule;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** data.sql 의 결제 룰 카탈로그(PAY_*) 재현. 배점이 바뀌면 여기와 data.sql 을 함께 고쳐야 한다. */
public final class PaymentRiskRules {

    /** 배점 룰 (rule_code -> score). */
    public static final Map<String, Integer> SCORES = new LinkedHashMap<>();

    /** 단축평가 전용 행. 점수 합산에 참여하지 않는다. */
    public static final List<String> PREFILTER_CODES = List.of("PAY_IMPOSSIBLE_TRAVEL");

    /** 송금 룰(rule_id 1~)과 구분되는 대역 — 혼합 카탈로그 테스트에서 충돌을 피한다. */
    private static final long RULE_ID_BASE = 100L;

    static {
        SCORES.put("PAY_SPLIT_PAYMENT", 40);
        SCORES.put("PAY_HIGH_AMOUNT_L3", 35);
        SCORES.put("PAY_RISKY_CATEGORY", 25);
        SCORES.put("PAY_PENDING_APPROVAL", 20);
        SCORES.put("PAY_HIGH_AMOUNT_L2", 18);
        SCORES.put("PAY_NIGHT_DEEP", 14);
        SCORES.put("PAY_HIGH_AMOUNT_L1", 10);
        SCORES.put("PAY_GIFT_CARD_AMOUNT", 10);
        SCORES.put("PAY_NIGHT_LATE", 6);
    }

    private PaymentRiskRules() {
    }

    /** 배점 룰 + 단축평가 행을 모두 담은 활성 룰 목록. */
    public static List<RiskRule> activeRules() {
        List<RiskRule> rules = new ArrayList<>();
        long id = RULE_ID_BASE + 1;
        for (Map.Entry<String, Integer> entry : SCORES.entrySet()) {
            rules.add(rule(id++, entry.getKey(), entry.getValue()));
        }
        for (String code : PREFILTER_CODES) {
            rules.add(rule(id++, code, 0));
        }
        return rules;
    }

    private static RiskRule rule(long id, String code, int score) {
        RiskRule rule = new RiskRule();
        rule.setRuleId(id);
        rule.setRuleCode(code);
        rule.setScore(score);
        rule.setActive(true);
        return rule;
    }
}
