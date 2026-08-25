package com.paywith.auth.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;

@ApiModel(description = "인증번호 확인 결과")
@Getter
public class PhoneVerifyResponse {

    @ApiModelProperty(value = "회원가입·비밀번호 재설정에 사용할 인증 토큰(하이픈 없는 UUID 32자 hex). 발급 후 5분 유효하며 "
        + "발급된 전화번호에만 유효하다. 같은 번호로 재검증하면 이전 토큰은 무효가 되고, 회원가입·비밀번호 재설정 "
        + "성공 시 소멸한다. 성공 응답에는 항상 포함된다", example = "3f2b8c1d9e4a4f6b8c2d1e0f9a8b7c6d")
    private final String verificationToken;

    public PhoneVerifyResponse(String verificationToken) {
        this.verificationToken = verificationToken;
    }
}
