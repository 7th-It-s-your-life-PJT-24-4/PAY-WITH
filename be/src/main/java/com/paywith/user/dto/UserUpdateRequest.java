package com.paywith.user.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@ApiModel(description = "사용자 정보 수정 요청(name·avatarId)")
@Getter
@Setter
public class UserUpdateRequest {

    @ApiModelProperty(value = "변경할 이름. 누락·공백은 400 REQUEST_001. 길이 검증은 없어(서버 검증 없음) 50자를 넘으면 DB 제약으로 500",
        required = true, example = "김시니어")
    @NotBlank
    private String name;

    @ApiModelProperty(value = "변경할 프리셋 아바타 번호(1~6). 생략(null) 시 기존 값 유지. 1~6 밖이면 400 REQUEST_001", example = "2")
    @Min(1)
    @Max(6)
    private Integer avatarId;
}
