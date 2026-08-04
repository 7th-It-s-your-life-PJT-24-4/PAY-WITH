package com.paywith.payment.scheduler;

import com.paywith.payment.service.PaymentExpiryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 결제 요청 만료 스캔의 실행 주기만 담당한다. 실제 종결 처리는 트랜잭션 경계를 갖는
 * {@link PaymentExpiryService} 가 수행한다. 다른 빈을 거쳐야 프록시가 적용돼
 * {@code @Transactional} 이 걸린다.
 */
@Component
public class PaymentExpiryScheduler {

    private static final Logger log = LoggerFactory.getLogger(PaymentExpiryScheduler.class);

    private final PaymentExpiryService paymentExpiryService;

    public PaymentExpiryScheduler(PaymentExpiryService paymentExpiryService) {
        this.paymentExpiryService = paymentExpiryService;
    }

    /**
     * {@code fixedDelay} 를 쓰는 이유는 이전 실행이 끝난 뒤부터 간격을 세기 위해서다.
     * {@code fixedRate} 는 스캔이 밀릴 때 실행이 겹쳐 같은 구간을 중복으로 훑는다.
     *
     * <p>예외를 밖으로 던지면 스케줄러가 다음 주기를 실행하지 않으므로 여기서 삼킨다.
     * 폴링·실행 경로에 조회 시점 lazy 전이가 남아 있어, 이 스캔이 몇 번 실패해도 만료된
     * QR로 결제되는 일은 없다.
     */
    @Scheduled(fixedDelayString = "${payment.expire-scan-ms:60000}")
    public void expireOverdue() {
        try {
            paymentExpiryService.expireOverdue();
        } catch (RuntimeException e) {
            log.error("결제 요청 만료 스캔 실패. 다음 주기에 다시 시도한다.", e);
        }
    }
}
