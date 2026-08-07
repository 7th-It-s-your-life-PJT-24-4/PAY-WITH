package com.paywith.guard.mapper;

import com.paywith.guard.domain.GuardSeniorRelation;
import com.paywith.guard.domain.GuardSummary;
import com.paywith.guard.domain.WardSummary;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface GuardSeniorMapper {

    boolean existsActiveRelation(@Param("guardId") Long guardId,
                                 @Param("seniorId") Long seniorId);

    boolean existsActivePairing(@Param("wardId") Long wardId);

    /** 신규 페어링이면 INSERT, 기존(REJECTED/REVOKED 등) 관계면 ACTIVE로 재연동한다. */
    int upsertActiveRelation(@Param("guardId") Long guardId, @Param("wardId") Long wardId);

    GuardSeniorRelation findRelation(@Param("guardId") Long guardId, @Param("wardId") Long wardId);

    int revokeActiveRelation(@Param("guardId") Long guardId, @Param("wardId") Long wardId);

    /** 보호자 홈 상단 탭용. 연동일 순으로 반환해 "생략 시 첫 번째 피보호자"를 안정적으로 정한다. */
    List<WardSummary> findActiveWards(@Param("guardId") Long guardId);

    /** 피보호자가 자신의 보호자 정보를 조회할 때 쓴다. 연동된 보호자가 없으면 null. */
    GuardSummary findActiveGuardByWardId(@Param("wardId") Long wardId);

}
