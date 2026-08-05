package com.paywith.payment.fds.service;

import com.paywith.merchant.domain.Merchant;
import com.paywith.payment.fds.rule.PaymentRuleContext;

public interface PaymentRuleContextCollector {

    /**
     * 결제 판정 입력을 수집한다. merchant는 실행 경로가 검증을 마치고 조회해 둔 행을 그대로
     * 받는다 — 여기서 가맹점을 다시 조회하지 않는다.
     */
    PaymentRuleContext collect(Long walletId, Long amount, Merchant merchant);
}
