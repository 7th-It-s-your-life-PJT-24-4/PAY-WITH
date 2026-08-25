package com.paywith.auth.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import javax.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@ApiModel(description = "비밀번호 재설정 요청")
@Getter
@Setter
public class PasswordResetRequest {

    @ApiModelProperty(value = "재설정할 계정의 전화번호. 비숫자를 제거한 뒤 01X + 숫자 7~8자리(총 10~11자리) 패턴을 검사하며 "
        + "불일치는 400 PHONE_001. 누락·공백은 400 REQUEST_001, 미가입 번호는 404 USER_002",
        required = true, example = "01012345678")
    @NotBlank
    private String phone;

    @ApiModelProperty(value = "인증번호 확인(POST /api/auth/phone/verify)으로 받은 verificationToken(하이픈 없는 UUID 32자 hex, "
        + "5분 유효). phone 으로 발급된 것이어야 하며 없거나 만료됐거나 다른 번호로 발급된 것이면 400 AUTH_003. "
        + "재설정 성공 시 소멸한다. 누락·공백은 400 REQUEST_001", required = true, example = "3f2b8c1d9e4a4f6b8c2d1e0f9a8b7c6d")
    @NotBlank
    private String verificationToken;

    @ApiModelProperty(value = "새 로그인 비밀번호. 8자 미만은 400 PASSWORD_001(회원가입 password 와 같은 규칙), "
        + "상한·문자 구성 검증은 없다. 누락·공백은 400 REQUEST_001. 성공 시 기존 refreshToken 이 무효화돼 "
        + "전 기기에서 재로그인이 필요하다", required = true, example = "newPassword1234")
    @NotBlank
    private String newPassword;
}