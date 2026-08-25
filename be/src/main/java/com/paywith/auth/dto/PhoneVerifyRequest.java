package com.paywith.auth.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import javax.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@ApiModel(description = "인증번호 확인 요청")
@Getter
@Setter
public class PhoneVerifyRequest {

    @ApiModelProperty(value = "인증번호를 받은 전화번호. 비숫자를 제거한 뒤 01X + 숫자 7~8자리(총 10~11자리) 패턴을 검사하며 "
        + "불일치는 400 PHONE_001. 누락·공백은 400 REQUEST_001", required = true, example = "01012345678")
    @NotBlank
    private String phone;

    @ApiModelProperty(value = "수신한 인증번호(숫자 6자리). 숫자 6자리가 아니면 400 PHONE_001, 인증번호가 만료·미발급이면 "
        + "400 AUTH_003, 불일치면 400 AUTH_003(실패 횟수 증가). 5회 틀린 뒤의 요청은 정답이어도 429 PHONE_003 이며 "
        + "인증번호가 무효화된다(재발송 필요). 누락·공백은 400 REQUEST_001", required = true, example = "482913")
    @NotBlank
    private String code;
}
