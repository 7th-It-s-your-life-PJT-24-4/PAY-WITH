package com.paywith.auth.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import javax.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@ApiModel(description = "인증번호 발송 요청")
@Getter
@Setter
public class PhoneCodeRequest {

    @ApiModelProperty(value = "인증번호를 받을 전화번호. 비숫자를 제거한 뒤 01X + 숫자 7~8자리(총 10~11자리) 패턴을 검사하며 "
        + "불일치는 400 PHONE_001. 누락·공백은 400 REQUEST_001. SIGNUP 인데 이미 가입된 번호면 409 USER_001, "
        + "PASSWORD_RESET 인데 미가입 번호면 404 USER_002. 같은 번호는 60초 재전송 쿨다운(429 PHONE_002, "
        + "data.retryAfter 초)과 24시간 5회 발송 한도(429 PHONE_003)가 적용된다", required = true, example = "01012345678")
    @NotBlank
    private String phone;

    @ApiModelProperty(value = "발송 용도. SIGNUP(회원가입) 또는 PASSWORD_RESET(비밀번호 찾기), 대소문자 구분. "
        + "그 외 값은 400(code 없음), 누락·공백은 400 REQUEST_001",
        required = true, allowableValues = "SIGNUP,PASSWORD_RESET", example = "SIGNUP")
    @NotBlank
    private String purpose;
}
