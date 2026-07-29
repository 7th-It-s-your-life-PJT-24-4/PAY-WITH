package com.paywith.fds.service.rule;

import com.paywith.fds.domain.RiskRule;
import com.paywith.fds.mapper.RiskRuleMapper;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class RiskRuleCache {

    private final RiskRuleMapper riskRuleMapper;

    // 새 컬렉션을 통째로 만들어 참조만 교체하므로, 조회하는 스레드가 갱신 중간 상태를 보는 일이 없다.
    private volatile List<RiskRule> activeRules = Collections.emptyList();
    private volatile Map<String, RiskRule> activeRulesByCode = Collections.emptyMap();

    public RiskRuleCache(RiskRuleMapper riskRuleMapper) {
        this.riskRuleMapper = riskRuleMapper;
        refresh();
    }

    public void refresh() {
        List<RiskRule> rules = riskRuleMapper.findAllActive();
        Map<String, RiskRule> byCode = new LinkedHashMap<>();
        for (RiskRule rule : rules) {
            byCode.put(rule.getRuleCode(), rule);
        }
        // 두 참조를 따로 교체해 그 사이에 불일치가 보일 수 있으나, 룰 카탈로그는 드물게 바뀌고
        // 판정은 행 단위로 독립적이라 문제되지 않는다.
        this.activeRules = Collections.unmodifiableList(rules);
        this.activeRulesByCode = Collections.unmodifiableMap(byCode);
    }

    public List<RiskRule> getActiveRules() {
        return activeRules;
    }

    public Optional<RiskRule> findByCode(String ruleCode) {
        return Optional.ofNullable(activeRulesByCode.get(ruleCode));
    }
}
