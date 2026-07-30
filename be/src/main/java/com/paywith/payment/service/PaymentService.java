package com.paywith.payment.service;

import com.paywith.exception.BusinessException;
import com.paywith.payment.domain.PaymentRequest;
import com.paywith.payment.domain.PaymentRequestStatus;
import com.paywith.payment.domain.PaymentWallet;
import com.paywith.payment.dto.PaymentCancelResponse;
import com.paywith.payment.dto.PaymentStatusResponse;
import com.paywith.payment.dto.QrCreateResponse;
import com.paywith.payment.mapper.PaymentRequestMapper;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentService {

    /** users.role — A5 확정으로 API·DB 모두 'WARD' (2026-07-30 스키마 반영, 매핑 불필요) */
    private static final String DB_ROLE_WARD = "WARD";
    private static final String WALLET_STATUS_LOCKED = "LOCKED";
    private static final String FAILURE_CODE_INSUFFICIENT_BALANCE = "INSUFFICIENT_BALANCE";

    private static final String TOKEN_PREFIX = "pay_qr_";
    private static final int TOKEN_RANDOM_LENGTH = 16;
    private static final String TOKEN_CHARACTERS =
        "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final int TOKEN_INSERT_MAX_ATTEMPTS = 3;

    private static final ZoneId ZONE_SEOUL = ZoneId.of("Asia/Seoul");

    private final PaymentRequestMapper paymentRequestMapper;
    private final PaymentTokenStore paymentTokenStore;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();

    public PaymentService(
        PaymentRequestMapper paymentRequestMapper,
        PaymentTokenStore paymentTokenStore,
        PasswordEncoder passwordEncoder
    ) {
        this.paymentRequestMapper = paymentRequestMapper;
        this.paymentTokenStore = paymentTokenStore;
        this.passwordEncoder = passwordEncoder;
    }

    /** QR 생성 — 검증(역할→페어링→지갑→비밀번호) 통과 후 토큰 생성 → INSERT → Redis SET 순서(A2) */
    @Transactional
    public QrCreateResponse createQr(Long userId, String pin) {
        requireWardRole(userId);

        if (!paymentRequestMapper.existsActivePairing(userId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "페어링 완료 후 이용할 수 있습니다.");
        }

        PaymentWallet wallet = paymentRequestMapper.findWalletByUserId(userId);
        if (wallet == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "지갑 정보를 찾을 수 없습니다.");
        }
        if (WALLET_STATUS_LOCKED.equals(wallet.getStatus())) {
            throw new BusinessException(HttpStatus.CONFLICT, "현재 거래가 제한된 지갑입니다.");
        }
        // 결제 비밀번호 = 송금 비밀번호와 동일한 users.pin(BCrypt) — QR 표시 전 본인 확인(A7)
        if (!passwordEncoder.matches(pin, wallet.getPin())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "결제 비밀번호가 올바르지 않습니다.");
        }

        PaymentRequest paymentRequest = insertWithNewToken(userId, wallet.getWalletId());
        paymentTokenStore.save(paymentRequest.getQrToken(), paymentRequest.getPaymentId());

        return new QrCreateResponse(
            paymentRequest.getPaymentId(),
            paymentRequest.getQrToken(),
            wallet.getBalance(),
            formatIso(paymentRequest.getExpiresAt()),
            PaymentTokenStore.QR_TTL_SECONDS
        );
    }

    /** 상태 폴링 — 소유자 검증 후 PENDING·만료 경과 건은 조회 시점에 EXPIRED로 lazy 전이 */
    @Transactional
    public PaymentStatusResponse getStatus(Long userId, Long paymentId) {
        requireWardRole(userId);

        PaymentRequest paymentRequest = findOwnedPaymentRequest(userId, paymentId);
        paymentRequest = transitionIfExpired(paymentRequest);

        return new PaymentStatusResponse(
            paymentRequest.getPaymentId(),
            paymentRequest.getTransactionId(),
            paymentRequest.getStatus().name(),
            paymentRequest.getMerchantName(),
            paymentRequest.getAmount(),
            formatIso(paymentRequest.getPaidAt()),
            paymentRequest.getRemainingBalance(),
            paymentRequest.getFailureCode(),
            failureMessageOf(paymentRequest.getFailureCode()),
            formatIso(paymentRequest.getExpiresAt())
        );
    }

    /** 취소 — PENDING만 취소 가능. PROCESSING/COMPLETED=409, EXPIRED=409, CANCELED는 멱등 응답 */
    @Transactional
    public PaymentCancelResponse cancel(Long userId, Long paymentId) {
        requireWardRole(userId);

        PaymentRequest paymentRequest = findOwnedPaymentRequest(userId, paymentId);
        paymentRequest = transitionIfExpired(paymentRequest);

        if (paymentRequest.getStatus() == PaymentRequestStatus.PENDING) {
            int canceled = paymentRequestMapper.cancelIfPending(paymentId);
            if (canceled == 1) {
                paymentTokenStore.delete(paymentRequest.getQrToken());
                PaymentRequest updated = paymentRequestMapper.findById(paymentId);
                return new PaymentCancelResponse(
                    updated.getPaymentId(),
                    updated.getStatus().name(),
                    formatIso(updated.getUpdatedAt())
                );
            }
            // 취소 직전에 만료·스캔 등으로 상태가 선점된 경우 — 최신 상태 기준으로 재판정
            paymentRequest = transitionIfExpired(paymentRequestMapper.findById(paymentId));
        }

        if (paymentRequest.getStatus() == PaymentRequestStatus.CANCELED) {
            // 이미 취소된 요청의 재취소는 멱등 처리
            return new PaymentCancelResponse(
                paymentRequest.getPaymentId(),
                paymentRequest.getStatus().name(),
                formatIso(paymentRequest.getUpdatedAt())
            );
        }
        if (paymentRequest.getStatus() == PaymentRequestStatus.EXPIRED) {
            throw new BusinessException(HttpStatus.CONFLICT, "이미 만료된 결제 요청입니다.");
        }
        throw new BusinessException(HttpStatus.CONFLICT, "이미 처리 중이거나 완료된 결제는 취소할 수 없습니다.");
    }

    private void requireWardRole(Long userId) {
        String role = paymentRequestMapper.findUserRole(userId);
        if (!DB_ROLE_WARD.equals(role)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "피보호자만 접근할 수 있습니다.");
        }
    }

    private PaymentRequest findOwnedPaymentRequest(Long userId, Long paymentId) {
        PaymentRequest paymentRequest = paymentRequestMapper.findById(paymentId);
        if (paymentRequest == null || !userId.equals(paymentRequest.getSeniorId())) {
            // 타인의 결제 요청은 존재 여부를 숨기기 위해 동일하게 404 처리
            throw new BusinessException(HttpStatus.NOT_FOUND, "결제 요청을 찾을 수 없습니다.");
        }
        return paymentRequest;
    }

    /** PENDING인데 expires_at이 지난 행을 EXPIRED로 전이. 경합 시(0건 갱신) 재조회 결과를 신뢰한다. */
    private PaymentRequest transitionIfExpired(PaymentRequest paymentRequest) {
        boolean expiredPending = paymentRequest.getStatus() == PaymentRequestStatus.PENDING
            && !paymentRequest.getExpiresAt().isAfter(LocalDateTime.now());
        if (!expiredPending) {
            return paymentRequest;
        }
        if (paymentRequestMapper.markExpiredIfPending(paymentRequest.getPaymentId()) == 1) {
            paymentRequest.setStatus(PaymentRequestStatus.EXPIRED);
            return paymentRequest;
        }
        return paymentRequestMapper.findById(paymentRequest.getPaymentId());
    }

    private PaymentRequest insertWithNewToken(Long userId, Long walletId) {
        DuplicateKeyException lastFailure = null;
        for (int attempt = 0; attempt < TOKEN_INSERT_MAX_ATTEMPTS; attempt++) {
            PaymentRequest paymentRequest = new PaymentRequest();
            paymentRequest.setWalletId(walletId);
            paymentRequest.setSeniorId(userId);
            paymentRequest.setQrToken(generateToken());
            paymentRequest.setStatus(PaymentRequestStatus.PENDING);
            paymentRequest.setExpiresAt(LocalDateTime.now().plusSeconds(PaymentTokenStore.QR_TTL_SECONDS));
            try {
                paymentRequestMapper.insert(paymentRequest);
                return paymentRequest;
            } catch (DuplicateKeyException exception) {
                // qr_token UNIQUE 충돌 — 새 토큰으로 재시도
                lastFailure = exception;
            }
        }
        throw lastFailure;
    }

    private String generateToken() {
        StringBuilder token = new StringBuilder(TOKEN_PREFIX);
        for (int i = 0; i < TOKEN_RANDOM_LENGTH; i++) {
            token.append(TOKEN_CHARACTERS.charAt(secureRandom.nextInt(TOKEN_CHARACTERS.length())));
        }
        return token.toString();
    }

    private String failureMessageOf(String failureCode) {
        if (failureCode == null) {
            return null;
        }
        if (FAILURE_CODE_INSUFFICIENT_BALANCE.equals(failureCode)) {
            return "결제 가능한 잔액이 부족합니다.";
        }
        return "결제에 실패했습니다.";
    }

    /** DB DATETIME(KST 기준)을 명세 형식(ISO 8601 +09:00 오프셋 포함)으로 변환 */
    private String formatIso(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.atZone(ZONE_SEOUL).toOffsetDateTime()
            .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }
}
