package com.paywith.guard.mapper;

import com.paywith.guard.domain.GuardSeniorRelation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface GuardSeniorMapper {

    boolean existsActiveRelation(@Param("guardId") Long guardId,
                                 @Param("seniorId") Long seniorId);

    /** 신규 페어링이면 INSERT, 기존(REJECTED/REVOKED 등) 관계면 ACTIVE로 재연동한다. */
    int upsertActiveRelation(@Param("guardId") Long guardId, @Param("wardId") Long wardId);

    GuardSeniorRelation findRelation(@Param("guardId") Long guardId, @Param("wardId") Long wardId);

}
