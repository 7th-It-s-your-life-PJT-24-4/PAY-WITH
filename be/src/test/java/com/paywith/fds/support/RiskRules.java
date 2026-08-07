package com.paywith.fds.support;

import com.paywith.fds.domain.RiskRule;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** R__seed.sql 의 룰 카탈로그 재현. 배점이 바뀌면 여기와 R__seed.sql 을 함께 고쳐야 한다. */
public final class RiskRules {

    /** 배점 룰 (rule_code -> score). */
    public static final Map<String, Integer> SCORES = new LinkedHashMap<>();

    /** 단축평가 전용 행. 룰 합산에는 참여하지 않고, 발동 시 이 배점이 총점이 된다. */
    public static final int PREFILTER_SCORE = 100;

    /** 단축평가 전용 행. */
    public static final List<String> PREFILTER_CODES = List.of("BL_REJECTED_RECIPIENT", "BL_FRAUD_ACCOUNT");

    static {
        SCORES.put("HIGH_AMOUNT_L3", 35);
        SCORES.put("DIVISION_TRANSFER", 28);
        SCORES.put("SUSPICIOUS_MEMO", 25);
        SCORES.put("HIGH_AMOUNT_L2", 18);
        SCORES.put("PENDING_APPROVAL_EXISTS", 20);
        SCORES.put("NEW_RECIPIENT", 15);
        SCORES.put("REPEATED", 14);
        SCORES.put("NIGHT_TIME_DEEP", 14);
        SCORES.put("HIGH_AMOUNT_L1", 10);
        SCORES.put("NIGHT_TIME_LATE", 6);
        SCORES.put("SAFE_ACCOUNT_CHECK", -20);
    }

    private RiskRules() {
    }

    /** 배점 룰 + 단축평가 행을 모두 담은 활성 룰 목록. rule_id 는 1부터 순번으로 부여한다. */
    public static List<RiskRule> activeRules() {
        List<RiskRule> rules = new ArrayList<>();
        long id = 1L;
        for (Map.Entry<String, Integer> entry : SCORES.entrySet()) {
            rules.add(rule(id++, entry.getKey(), entry.getValue()));
        }
        for (String code : PREFILTER_CODES) {
            rules.add(rule(id++, code, PREFILTER_SCORE));
        }
        return rules;
    }

    /** rule_id -> rule_code 역인덱스. 발동 룰 이름 복원용. */
    public static Map<Long, String> codeById(List<RiskRule> rules) {
        Map<Long, String> byId = new LinkedHashMap<>();
        rules.forEach(rule -> byId.put(rule.getRuleId(), rule.getRuleCode()));
        return byId;
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
