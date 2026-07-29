package com.paywith.fds.mapper;

import com.paywith.fds.domain.RiskEvaluationDetail;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface RiskEvaluationDetailMapper {

    int insert(RiskEvaluationDetail riskEvaluationDetail);
}
