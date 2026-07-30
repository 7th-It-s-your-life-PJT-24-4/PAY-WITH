package com.paywith.payment.domain;

import lombok.Getter;
import lombok.Setter;

/**
 * 결제 파트 전용 지갑 조회 뷰. wallets 테이블의 소유·수정은 지갑 공통 파트 담당이므로
 * 결제 검증에 필요한 최소 컬럼만 읽기 전용으로 사용한다.
 */
@Getter
@Setter
public class PaymentWallet {

    private Long walletId;
    private Long balance;
    private String status;
    private String pin;
}
