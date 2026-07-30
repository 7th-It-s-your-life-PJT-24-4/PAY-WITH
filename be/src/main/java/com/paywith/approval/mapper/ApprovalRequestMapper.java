package com.paywith.approval.mapper;

import com.paywith.approval.domain.ApprovalRequest;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ApprovalRequestMapper {

    int insert(ApprovalRequest approvalRequest);
}
