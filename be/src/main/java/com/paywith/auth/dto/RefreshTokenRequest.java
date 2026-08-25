package com.paywith.auth.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import javax.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@ApiModel(description = "토큰 재발급 요청")
@Getter
@Setter
public class RefreshTokenRequest {

    @ApiModelProperty(value = "로그인·재발급 응답으로 받은 refreshToken(JWT). 누락·공백은 400 REQUEST_001. 서명 불일치·만료, "
        + "서버에 저장된 최신 값과 불일치(이전 토큰 재사용 등), 사용자 탈퇴(WITHDRAWN)는 401(code 없음, 메시지는 각각 다름). "
        + "accessToken 을 넣으면 서명 검증은 통과하나 저장값과 달라 401", required = true, example = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIiwicGhvbmUiOiIwMTAxMjM0NTY3OCIsImlhdCI6MTc4Nzk5MDQwMCwiZXhwIjoxNzg5MjAwMDAwfQ.qM4nB7vC1xZ5aS9dF3gH6jK0lP2rT8wY4eU7iO1pA3s")
    @NotBlank
    private String refreshToken;
}
