package com.paywith.notification.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 송금 알림을 만드는 데 필요한 최소 정보. 송금 FDS·승인 흐름은 transactionId 만 들고 있어서
 * 수신자(피보호자)도 문구에 넣을 금액·수취인도 여기서 한 번에 가져온다.
 */
@Getter
@Setter
@NoArgsConstructor
public class TransferNotificationInfo {

    /** 거래의 주인. 보호자를 찾을 때의 기준이자 본인에게 보낼 때의 수신자다. */
    private Long wardId;

    private Long amount;

    /** 수취인 예금주명. 충전처럼 수취인이 없는 거래면 null. */
    private String recipientName;
}
