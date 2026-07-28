package com.paywith.fds.mapper;

import com.paywith.fds.domain.RiskEvaluation;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface RiskEvaluationMapper {

    int insert(RiskEvaluation riskEvaluation);
}
