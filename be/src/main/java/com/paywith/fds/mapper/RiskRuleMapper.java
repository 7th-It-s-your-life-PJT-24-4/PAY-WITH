package com.paywith.fds.mapper;

import com.paywith.fds.domain.RiskRule;
import java.util.List;

public interface RiskRuleMapper {

    List<RiskRule> findAllActive();
}
