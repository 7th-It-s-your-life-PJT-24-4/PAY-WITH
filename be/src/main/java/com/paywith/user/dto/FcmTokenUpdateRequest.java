package com.paywith.user.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FcmTokenUpdateRequest {

    /**
     * users.fcm_token 이 VARCHAR(255) 라 길이를 여기서 막는다. sql_mode 에 STRICT_TRANS_TABLES
     * 가 있어 초과분이 잘려 저장되지는 않지만, 그대로 두면 DB 가 던지는 1406 이 500 으로
     * 나가 원인을 알 수 없는 실패가 된다. 여기서 막아 400 과 명확한 메시지로 돌려준다.
     *
     * <p>실제 FCM 등록 토큰은 160 자 안팎이라 지금은 여유가 있다. 구글이 최대 길이를 보장하지
     * 않으므로 언젠가 이 검증에 걸릴 수 있는데, 그때는 조용히 실패하지 않고 400 으로 드러나므로
     * 그 시점에 컬럼을 넓히면 된다.
     */
    @NotBlank(message = "FCM 토큰을 입력해주세요.")
    @Size(max = 255, message = "FCM 토큰이 너무 깁니다.")
    private String fcmToken;
}
