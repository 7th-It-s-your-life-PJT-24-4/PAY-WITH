package com.paywith.payment.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ExecuteRequest {

    /** QR에서 읽은 60초 1회용 결제 토큰 */
    @NotBlank(message = "QR 토큰을 입력해주세요.")
    private String qrToken;

    /** 결제 가맹점 번호 — 좌표 등 가맹점 정보는 서버 조회값만 사용(클라이언트 좌표 전달 금지) */
    @NotNull(message = "가맹점 번호를 입력해주세요.")
    @Positive(message = "가맹점 번호가 올바르지 않습니다.")
    private Long merchantId;

    @NotNull(message = "결제 금액을 입력해주세요.")
    @Positive(message = "결제 금액은 1원 이상이어야 합니다.")
    private Long amount;
}
