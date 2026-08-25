package com.paywith.user.dto;

import com.paywith.user.domain.User;
import com.paywith.user.domain.Role;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.time.LocalDateTime;
import lombok.Getter;

@ApiModel(description = "사용자 정보(회원가입·조회·수정 응답). status·birthDate·gender 는 포함하지 않는다")
@Getter
public class UserResponse {

    @ApiModelProperty(value = "사용자 ID(users.user_id)", example = "1")
    private final Long id;

    @ApiModelProperty(value = "전화번호. 가입 시 비숫자를 제거한 숫자만 저장된 값", example = "01012345678")
    private final String phone;

    @ApiModelProperty(value = "이름", example = "김시니어")
    private final String name;

    @ApiModelProperty(value = "사용자 역할. WARD=피보호자, GUARD=보호자", allowableValues = "WARD,GUARD", example = "WARD")
    private final Role role;

    @ApiModelProperty(value = "프리셋 아바타 번호(1~6). 가입 시 생략했으면 1. DB NOT NULL 이라 null 없음", example = "1")
    private final Integer avatarId;

    @ApiModelProperty(value = "가입 시각(DB created_at, 초 단위). 오프셋 없는 ISO 형식이며 null 없음", example = "2026-08-25T15:00:00")
    private final LocalDateTime createdAt;

    @ApiModelProperty(value = "마지막 수정 시각(DB updated_at, 수정 시 자동 갱신). 가입 직후에는 createdAt 과 같다. "
        + "오프셋 없는 ISO 형식이며 null 없음", example = "2026-08-25T15:00:00")
    private final LocalDateTime updatedAt;

    public UserResponse(User user) {
        this.id = user.getId();
        this.phone = user.getPhone();
        this.name = user.getName();
        this.role = user.getRole();
        this.avatarId = user.getAvatarId();
        this.createdAt = user.getCreatedAt();
        this.updatedAt = user.getUpdatedAt();
    }
}
