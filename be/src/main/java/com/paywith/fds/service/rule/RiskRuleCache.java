package com.paywith.fds.service.rule;

import com.paywith.fds.domain.RiskRule;
import com.paywith.fds.mapper.RiskRuleMapper;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class RiskRuleCache {

    private final RiskRuleMapper riskRuleMapper;

    // 새 리스트를 통째로 만들어 참조만 교체하므로, 조회하는 스레드가 갱신 중간 상태를 보는 일이 없다.
    private volatile List<RiskRule> activeRules = Collections.emptyList();

    public RiskRuleCache(RiskRuleMapper riskRuleMapper) {
        this.riskRuleMapper = riskRuleMapper;
        refresh();
    }

    public void refresh() {
        this.activeRules = Collections.unmodifiableList(riskRuleMapper.findAllActive());
    }

    public List<RiskRule> getActiveRules() {
        return activeRules;
    }
}
