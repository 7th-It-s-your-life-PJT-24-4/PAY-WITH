package com.paywith.user.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@ApiModel(description = "FCM 토큰 등록·해제 요청")
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
    @ApiModelProperty(value = "기기의 FCM 등록 토큰. 누락·공백 또는 255자 초과는 400 REQUEST_001(users.fcm_token VARCHAR(255)). "
        + "등록(PUT)은 사용자당 1개로 덮어쓰며 같은 토큰이 다른 사용자에게 등록돼 있으면 그쪽 등록을 먼저 해제한다"
        + "(fcm_token 유니크). 해제(DELETE)는 현재 등록된 토큰과 같을 때만 지우고, 달라도 200(멱등)",
        required = true, example = "fZ8kQ3xT9Uo:APA91bHq7xL2mN4pR6sT8vW0yA1cE3gI5kM7oQ9sU1wY3aC5eG7iK9mO1qS3uW5yA7cE9gI1kM3oQ5sU7wY9aC1eG3iK5mO7qS9uW")
    @NotBlank(message = "FCM 토큰을 입력해주세요.")
    @Size(max = 255, message = "FCM 토큰이 너무 깁니다.")
    private String fcmToken;
}
