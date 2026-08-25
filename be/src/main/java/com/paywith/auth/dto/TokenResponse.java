package com.paywith.auth.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;

@ApiModel(description = "토큰 발급 결과(로그인·재발급 공통)")
@Getter
public class TokenResponse {

    @ApiModelProperty(value = "API 호출용 액세스 토큰(JWT HS256, sub=사용자 ID, claim phone). Authorization: Bearer <accessToken> "
        + "헤더로 보낸다. 유효 시간은 운영 15분·로컬 30분(JWT_ACCESS_TOKEN_VALIDITY_MS 로 조정)", example = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIiwicGhvbmUiOiIwMTAxMjM0NTY3OCIsImlhdCI6MTc4Nzk5MDQwMCwiZXhwIjoxNzg3OTkxMzAwfQ.x7Kq2mP9vL4wN8sR1tU6yB3cE5gH0jM_aD-fZ8kQ3xT")
    private final String accessToken;

    @ApiModelProperty(value = "재발급용 리프레시 토큰(accessToken 과 같은 구조의 JWT, 유효 14일). 사용자당 1개만 서버에 저장되며 "
        + "로그인·재발급마다 새 값으로 교체돼 이전 refreshToken 은 즉시 무효가 된다", example = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIiwicGhvbmUiOiIwMTAxMjM0NTY3OCIsImlhdCI6MTc4Nzk5MDQwMCwiZXhwIjoxNzg5MjAwMDAwfQ.qM4nB7vC1xZ5aS9dF3gH6jK0lP2rT8wY4eU7iO1pA3s")
    private final String refreshToken;

    @ApiModelProperty(value = "토큰 타입. 항상 \"Bearer\"", allowableValues = "Bearer", example = "Bearer")
    private final String tokenType;

    public TokenResponse(String accessToken, String refreshToken) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.tokenType = "Bearer";
    }
}
