package com.paywith.user.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FcmTokenUpdateRequest {

    /**
     * users.fcm_token 이 VARCHAR(255) 라 길이를 여기서 막는다. RDS 가 비엄격 sql_mode 라
     * 초과분은 에러 없이 잘려 저장되고, 잘린 토큰으로는 발송이 조용히 실패한다.
     */
    @NotBlank(message = "FCM 토큰을 입력해주세요.")
    @Size(max = 255, message = "FCM 토큰이 너무 깁니다.")
    private String fcmToken;
}
