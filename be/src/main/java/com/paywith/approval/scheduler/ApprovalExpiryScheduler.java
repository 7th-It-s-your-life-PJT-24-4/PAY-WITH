package com.paywith.approval.scheduler;

import com.paywith.approval.service.ApprovalExpiryService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 승인요청 만료 스캔의 실행 주기만 담당한다. 실제 종결 처리는 트랜잭션 경계를 갖는
 * {@link ApprovalExpiryService} 가 수행한다. 다른 빈을 거쳐야 프록시가 적용돼
 * {@code @Transactional} 이 걸린다.
 */
@Component
@RequiredArgsConstructor
public class ApprovalExpiryScheduler {

    private static final Logger log = LoggerFactory.getLogger(ApprovalExpiryScheduler.class);

    private final ApprovalExpiryService approvalExpiryService;

    /**
     * {@code fixedDelay} 를 쓰는 이유는 이전 실행이 끝난 뒤부터 간격을 세기 위해서다.
     * {@code fixedRate} 는 스캔이 밀릴 때 실행이 겹쳐 같은 구간을 중복으로 훑는다.
     *
     * <p>예외를 밖으로 던지면 스케줄러가 다음 주기를 실행하지 않으므로 여기서 삼킨다.
     * 만료 판정은 조회 쪽에서도 {@code expired_at} 으로 한 번 더 거르기 때문에, 이 스캔이
     * 몇 번 실패해도 만료된 건이 승인되는 일은 없다.
     */
    @Scheduled(fixedDelayString = "${fds.approval.expire-scan-ms:60000}")
    public void expireOverdue() {
        try {
            approvalExpiryService.expireOverdue();
        } catch (RuntimeException e) {
            log.error("승인요청 만료 스캔 실패. 다음 주기에 다시 시도한다.", e);
        }
    }
}
