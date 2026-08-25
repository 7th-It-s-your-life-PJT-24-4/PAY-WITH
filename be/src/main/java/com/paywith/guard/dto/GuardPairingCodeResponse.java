package com.paywith.guard.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.time.LocalDateTime;
import lombok.Getter;

@ApiModel(description = "페어링 코드 발급 결과")
@Getter
public class GuardPairingCodeResponse {

    @ApiModelProperty(value = "피보호자에게 전달할 인증 코드. 숫자 5자리 문자열(앞자리 0 포함, SecureRandom 생성). "
            + "발급 후 5분간 유효하며 재발급하면 이전 코드는 즉시 무효화", example = "72941")
    private final String code;
    @ApiModelProperty(value = "초대 링크. {pairing.invite-base-url}/{code} 형식"
            + "(base URL 기본값 https://paywith.link, 환경변수 PAIRING_INVITE_BASE_URL로 변경)",
            example = "https://paywith.link/72941")
    private final String inviteUrl;
    @ApiModelProperty(value = "코드 만료 시각(서버 시각 기준 발급 시각 + 5분, 초 단위 절삭). 오프셋 없는 yyyy-MM-dd'T'HH:mm:ss 형식",
            example = "2026-08-25T15:05:00")
    private final LocalDateTime expiresAt;

    public GuardPairingCodeResponse(String code, String inviteUrl, LocalDateTime expiresAt) {
        this.code = code;
        this.inviteUrl = inviteUrl;
        this.expiresAt = expiresAt;
    }
}