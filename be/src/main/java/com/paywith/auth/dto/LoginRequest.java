package com.paywith.auth.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import javax.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@ApiModel(description = "로그인 요청")
@Getter
@Setter
public class LoginRequest {

    @ApiModelProperty(value = "로그인할 전화번호. 비숫자를 제거한 뒤 가입 전화번호와 대조하므로 하이픈 포함 입력도 수용되며 "
        + "형식 검증은 없다(서버 검증 없음). 누락·공백은 400 REQUEST_001", required = true, example = "01012345678")
    @NotBlank
    private String phone;

    @ApiModelProperty(value = "로그인 비밀번호(가입 시 password, BCrypt 대조). 길이·형식 검증은 없다(서버 검증 없음). "
        + "누락·공백은 400 REQUEST_001. 미가입·탈퇴(WITHDRAWN)·불일치는 모두 같은 메시지의 401(code 없음)",
        required = true, example = "password1234")
    @NotBlank
    private String password;
}
