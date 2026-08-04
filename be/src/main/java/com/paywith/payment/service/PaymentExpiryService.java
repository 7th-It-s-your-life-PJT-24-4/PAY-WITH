package com.paywith.payment.service;

import com.paywith.payment.mapper.PaymentRequestMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 기한이 지난 QR 결제 요청의 종결 처리.
 *
 * <p>피보호자가 직접 일으키는 생성·취소({@link PaymentService})와 달리 시스템이 시간에 따라
 * 수행하는 작업이라 서비스를 나눴다. 폴링·실행 경로의 lazy 전이는 요청이 닿는 건만 종결할 수
 * 있으므로, QR 생성 후 페이지 이탈 등으로 아무 요청도 닿지 않는 방치 건은 이 스캔이 종결한다.
 */
@Service
public class PaymentExpiryService {

    private static final Logger log = LoggerFactory.getLogger(PaymentExpiryService.class);

    private final PaymentRequestMapper paymentRequestMapper;

    public PaymentExpiryService(PaymentRequestMapper paymentRequestMapper) {
        this.paymentRequestMapper = paymentRequestMapper;
    }

    /**
     * PENDING이면서 expires_at이 지난 건을 일괄 EXPIRED로 전이한다.
     *
     * <p>조건부 UPDATE라 폴링·취소·실행의 개별 전이와 겹쳐도 한쪽만 성공한다. 별도 잠금은
     * 필요 없고, Redis qr:{token} 키는 TTL 60초로 스스로 사라지므로 여기서 지우지 않는다.
     *
     * @return 만료 처리한 결제 요청 수
     */
    @Transactional
    public int expireOverdue() {
        int expired = paymentRequestMapper.expireOverdue();
        if (expired > 0) {
            log.info("결제 요청 만료 처리 {}건", expired);
        }
        return expired;
    }
}
