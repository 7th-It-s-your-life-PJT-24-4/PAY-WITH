package com.paywith.payment.service;

import java.time.Duration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * QR 토큰 Redis 보조 저장소 전담 (key: qr:{token} → value: paymentId, TTL 60초).
 * Redis 값은 만료·유실될 수 있으므로 결제 상태의 최종 기준은 항상 payment_requests(MySQL)다(A2).
 */
@Component
public class PaymentTokenStore {

    /** QR 토큰 유효시간(초) — 명세 확정 60초. expires_at 계산도 이 값을 사용한다. */
    public static final int QR_TTL_SECONDS = 60;

    private static final String KEY_PREFIX = "qr:";

    private final StringRedisTemplate redisTemplate;

    public PaymentTokenStore(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void save(String qrToken, Long paymentId) {
        redisTemplate.opsForValue()
            .set(KEY_PREFIX + qrToken, String.valueOf(paymentId), Duration.ofSeconds(QR_TTL_SECONDS));
    }

    /**
     * 결제 실행 시 1회용 소비(GETDEL). 반환값이 없어도 만료로 단정하지 말고
     * MySQL(findByToken)로 최종 판단해야 한다 — PR#3 결제 실행에서 사용.
     */
    public Long consume(String qrToken) {
        String paymentId = redisTemplate.opsForValue().getAndDelete(KEY_PREFIX + qrToken);
        return paymentId == null ? null : Long.valueOf(paymentId);
    }

    /** 취소 시 토큰 즉시 무효화 */
    public void delete(String qrToken) {
        redisTemplate.delete(KEY_PREFIX + qrToken);
    }
}
