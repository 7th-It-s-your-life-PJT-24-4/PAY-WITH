package com.paywith.guard.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface GuardSeniorMapper {

    boolean existsActiveRelation(@Param("guardId") Long guardId,
                                 @Param("seniorId") Long seniorId);

}
