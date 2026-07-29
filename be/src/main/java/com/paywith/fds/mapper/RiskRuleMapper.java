package com.paywith.fds.mapper;

import com.paywith.fds.domain.RiskRule;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface RiskRuleMapper {

    List<RiskRule> findAllActive();
}
