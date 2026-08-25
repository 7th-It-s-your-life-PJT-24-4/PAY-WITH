package com.paywith.guard.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.time.LocalDateTime;
import lombok.Getter;

@ApiModel(description = "보호자-피보호자 연동 결과. 피보호자 코드 즉시 연결(201)과 보호자 확인 요청 승인(200)이 같은 구조로 응답")
@Getter
public class WardPairingResponse {

    @ApiModelProperty(value = "guard_senior 관계 ID. 같은 보호자-피보호자 쌍은 해제 후 재페어링해도 동일 ID 재사용"
            + "(UNIQUE (guard_id, senior_id) upsert)", example = "1")
    private final Long relationId;
    @ApiModelProperty(value = "연결된 보호자 회원 ID(users.user_id)", example = "2")
    private final Long guardId;
    @ApiModelProperty(value = "연결된 보호자 이름(users.name, 최대 50자)", example = "김보호")
    private final String guardName;
    @ApiModelProperty(value = "연동 상태. 연동 직후 조회한 값이라 항상 ACTIVE", allowableValues = "ACTIVE", example = "ACTIVE")
    private final String status;
    @ApiModelProperty(value = "연동 확정 시각(DB NOW() 기록, 재페어링 시 갱신). 오프셋 없는 yyyy-MM-dd'T'HH:mm:ss 형식",
            example = "2026-08-25T15:00:00")
    private final LocalDateTime connectedAt;

    public WardPairingResponse(Long relationId, Long guardId, String guardName, String status, LocalDateTime connectedAt) {
        this.relationId = relationId;
        this.guardId = guardId;
        this.guardName = guardName;
        this.status = status;
        this.connectedAt = connectedAt;
    }
}