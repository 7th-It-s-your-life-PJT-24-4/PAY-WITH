package com.paywith.payment.service;

import com.paywith.exception.BusinessException;
import com.paywith.fds.domain.RiskLevel;
import com.paywith.fds.dto.FdsDecision;
import com.paywith.merchant.domain.Merchant;
import com.paywith.merchant.mapper.MerchantMapper;
import com.paywith.notification.domain.NotificationType;
import com.paywith.notification.service.NotificationService;
import com.paywith.payment.domain.PaymentRequest;
import com.paywith.payment.domain.PaymentRequestStatus;
import com.paywith.payment.domain.PaymentWallet;
import com.paywith.payment.dto.ExecuteRequest;
import com.paywith.payment.dto.ExecuteResponse;
import com.paywith.payment.fds.service.PaymentFdsEvaluationService;
import com.paywith.payment.fds.service.PaymentFdsResultService;
import com.paywith.payment.mapper.PaymentRequestMapper;
import com.paywith.transaction.domain.Transaction;
import com.paywith.transaction.domain.TransactionStatus;
import com.paywith.transaction.domain.TransactionType;
import com.paywith.transaction.mapper.TransactionMapper;
import com.paywith.wallet.mapper.WalletMapper;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 결제 실행 오케스트레이션 — 토큰 소비(Redis GETDEL + MySQL 폴백) → 가맹점 검증 →
 * payment_requests 행잠금·최종 확인 → 지갑 차감·원장 기록을 단일 DB 트랜잭션으로 수행한다.
 *
 * 잠금 순서 규약: payment_requests → wallets. 송금·충전 등 지갑을 잠그는 다른 경로와의
 * 데드락을 막기 위해 이 순서를 바꾸지 않는다.
 *
 * 실패 종결(FAILED·EXPIRED)은 기록이 커밋으로 남아야 하므로 트랜잭션 안에서 예외를 던지지
 * 않고(던지면 롤백으로 기록이 사라진다) ExecuteResult.error로 반환해 커밋 후에 던진다.
 */
@Service
public class PaymentExecuteService {

    private static final String FAILURE_CODE_INSUFFICIENT_BALANCE = "INSUFFICIENT_BALANCE";
    private static final String FAILURE_CODE_FDS_BLOCKED = "FDS_BLOCKED";
    private static final String TRANSACTION_STATUS_COMPLETED = "COMPLETED";

    private static final String MESSAGE_INVALID_TOKEN = "유효하지 않은 QR입니다. 새 QR로 다시 시도해주세요.";
    private static final String MESSAGE_INSUFFICIENT_BALANCE = "결제 가능한 잔액이 부족합니다.";
    // 발동 룰·점수는 응답에 싣지 않는다 — 스캐너는 제3자라 차단 사유를 학습할 수 없어야 한다
    private static final String MESSAGE_FDS_BLOCKED = "결제가 차단되었습니다. 보호자에게 문의해 주세요.";

    private static final String NOTIFICATION_REF_TYPE_TRANSACTION = "TRANSACTION";
    private static final String NOTIFICATION_TITLE_BLOCKED = "결제 차단 알림";
    private static final String NOTIFICATION_BODY_BLOCKED = "위험 거래가 감지되어 %s %,d원 결제를 차단했습니다.";
    private static final String NOTIFICATION_TITLE_CAUTION = "결제 주의 알림";
    private static final String NOTIFICATION_BODY_CAUTION =
        "주의가 필요한 결제가 감지되었습니다. %s %,d원 결제가 정상 완료되었습니다.";

    private static final ZoneId ZONE_SEOUL = ZoneId.of("Asia/Seoul");

    private final PaymentRequestMapper paymentRequestMapper;
    private final MerchantMapper merchantMapper;
    private final WalletMapper walletMapper;
    private final TransactionMapper transactionMapper;
    private final PaymentTokenStore paymentTokenStore;
    private final PaymentFdsEvaluationService paymentFdsEvaluationService;
    private final PaymentFdsResultService paymentFdsResultService;
    private final NotificationService notificationService;
    private final TransactionTemplate transactionTemplate;

    public PaymentExecuteService(
        PaymentRequestMapper paymentRequestMapper,
        MerchantMapper merchantMapper,
        WalletMapper walletMapper,
        TransactionMapper transactionMapper,
        PaymentTokenStore paymentTokenStore,
        PaymentFdsEvaluationService paymentFdsEvaluationService,
        PaymentFdsResultService paymentFdsResultService,
        NotificationService notificationService,
        PlatformTransactionManager transactionManager
    ) {
        this.paymentRequestMapper = paymentRequestMapper;
        this.merchantMapper = merchantMapper;
        this.walletMapper = walletMapper;
        this.transactionMapper = transactionMapper;
        this.paymentTokenStore = paymentTokenStore;
        this.paymentFdsEvaluationService = paymentFdsEvaluationService;
        this.paymentFdsResultService = paymentFdsResultService;
        this.notificationService = notificationService;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    public ExecuteResponse execute(ExecuteRequest request) {
        // ① Redis 1회용 소비 — 미스여도 만료로 단정하지 않고 최종 기준인 MySQL로 판단한다
        if (paymentTokenStore.consume(request.getQrToken()) == null) {
            ExecuteResponse settled = settleFromDatabase(request.getQrToken());
            if (settled != null) {
                return settled;
            }
        }

        ExecuteResult result = transactionTemplate.execute(status -> executeInTransaction(request));
        if (result.error != null) {
            throw result.error;
        }
        return result.response;
    }

    /**
     * Redis 미스 시 MySQL 기준 선별 — 완료·실패로 종결된 건은 기존 결과를 멱등 반환해
     * 가맹점 재시도를 안전하게 만들고, PENDING·미만료 건만 결제 진행(null)으로 넘긴다.
     */
    private ExecuteResponse settleFromDatabase(String qrToken) {
        PaymentRequest settledView = paymentRequestMapper.findByToken(qrToken);
        if (settledView == null) {
            throw invalidTokenException();
        }
        switch (settledView.getStatus()) {
            case COMPLETED:
                return settledResponse(settledView);
            case FAILED:
                throw settledFailureException(settledView.getFailureCode());
            case PENDING:
                if (settledView.getExpiresAt().isAfter(LocalDateTime.now())) {
                    return null;
                }
                // 폴링과 동일한 lazy 만료 전이 — 조건부 UPDATE라 경합해도 안전하다
                paymentRequestMapper.markExpiredIfPending(settledView.getPaymentId());
                throw invalidTokenException();
            default: // PROCESSING·EXPIRED·CANCELED — 이미 사용됐거나 무효화된 토큰
                throw invalidTokenException();
        }
    }

    private ExecuteResult executeInTransaction(ExecuteRequest request) {
        // ② 가맹점 검증 — 이름·좌표는 서버 조회값만 사용한다(클라이언트 좌표 전달 금지)
        Merchant merchant = merchantMapper.findById(request.getMerchantId());
        if (merchant == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "MERCHANT_001", "가맹점을 찾을 수 없습니다.");
        }
        if (merchant.getLatitude() == null || merchant.getLongitude() == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "MERCHANT_001", "결제할 수 없는 가맹점입니다.");
        }

        // ②′ PAY-FDS 수집·판정 — 행잠금(③) 전에 끝내 잠금 보유 시간을 늘리지 않는다.
        // 판정~잠금 사이 다른 결제가 끼어들 수 있으나 반복 룰 집계 1건 오차 수준이라 수용한다.
        // 내부 오류는 evaluate가 fail-open(SAFE 폴백)으로 흡수하므로 여기서 방어하지 않는다.
        FdsDecision fdsDecision = evaluateFds(request, merchant);

        // ③ 행잠금 후 최종 확인 — 행 존재 / 완료·실패 멱등 / 상태 / 만료 / 소유자 정합
        PaymentRequest paymentRequest = paymentRequestMapper.findByTokenForUpdate(request.getQrToken());
        if (paymentRequest == null) {
            throw invalidTokenException();
        }
        if (paymentRequest.getStatus() == PaymentRequestStatus.COMPLETED) {
            // Redis 히트 직후 다른 실행이 먼저 완료한 경우 — 기존 결과 멱등 반환
            return ExecuteResult.success(settledResponse(paymentRequestMapper.findByToken(request.getQrToken())));
        }
        if (paymentRequest.getStatus() == PaymentRequestStatus.FAILED) {
            throw settledFailureException(paymentRequest.getFailureCode());
        }
        if (paymentRequest.getStatus() != PaymentRequestStatus.PENDING) {
            throw invalidTokenException();
        }
        if (!paymentRequest.getExpiresAt().isAfter(LocalDateTime.now())) {
            // 만료 기록은 커밋으로 남기고 오류 응답은 커밋 후에 던진다
            paymentRequestMapper.markExpiredIfPending(paymentRequest.getPaymentId());
            return ExecuteResult.failure(invalidTokenException());
        }
        PaymentWallet wallet = paymentRequestMapper.findWalletByUserId(paymentRequest.getSeniorId());
        if (wallet == null || !wallet.getWalletId().equals(paymentRequest.getWalletId())) {
            throw invalidTokenException();
        }

        // PROCESSING 전이 + 스캔 시점의 가맹점·금액 확정
        if (paymentRequestMapper.markProcessing(
            paymentRequest.getPaymentId(), merchant.getMerchantId(), request.getAmount()) != 1) {
            // 행잠금 하에서는 도달할 수 없는 경합 — 방어적으로 무효 토큰 처리
            throw invalidTokenException();
        }

        // ④ 판정 분기 — DANGER 는 차감 없이 증적만 커밋하고 커밋 후 403 을 던진다
        if (fdsDecision != null && fdsDecision.getRiskLevel() == RiskLevel.DANGER) {
            return blockPayment(paymentRequest, merchant, request.getAmount(), fdsDecision);
        }

        // 지갑 차감(조건부 원자 UPDATE) — 0건이면 잔액 부족
        if (walletMapper.decreaseBalanceIfSufficient(wallet.getWalletId(), request.getAmount()) == 0) {
            paymentRequestMapper.failPayment(paymentRequest.getPaymentId(), FAILURE_CODE_INSUFFICIENT_BALANCE);
            return ExecuteResult.failure(
                new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, "WALLET_003", MESSAGE_INSUFFICIENT_BALANCE));
        }

        // 차감 후 같은 트랜잭션 내 재조회로 balance_after 확정 (송금 구현과 동일 패턴)
        Long balanceAfter = paymentRequestMapper.findWalletByUserId(paymentRequest.getSeniorId()).getBalance();
        LocalDateTime paidAt = LocalDateTime.now();
        Transaction transaction = Transaction.builder()
            .walletId(wallet.getWalletId())
            .merchantId(merchant.getMerchantId())
            .type(TransactionType.PAYMENT)
            .amount(request.getAmount())
            .balanceAfter(balanceAfter)
            .status(TransactionStatus.COMPLETED)
            .latitude(merchant.getLatitude().doubleValue())
            .longitude(merchant.getLongitude().doubleValue())
            .createdAt(paidAt)
            .completedAt(paidAt)
            .build();
        transactionMapper.insertTransaction(transaction);

        if (paymentRequestMapper.completePayment(
            paymentRequest.getPaymentId(), transaction.getTransactionId()) != 1) {
            // 행잠금 하에서 완료 전이 실패는 정합 훼손 — 예외로 전체 롤백시킨다
            throw new IllegalStateException(
                "결제 완료 전이에 실패했습니다. paymentId=" + paymentRequest.getPaymentId());
        }

        // 판정 기록은 거래 행 확정 후에만 가능하다. CAUTION 은 결제는 그대로 두고 알림만 남긴다.
        if (fdsDecision != null) {
            paymentFdsResultService.save(transaction.getTransactionId(), fdsDecision);
            if (fdsDecision.getRiskLevel() == RiskLevel.CAUTION) {
                notificationService.notifyGuardians(
                    paymentRequest.getSeniorId(),
                    NotificationType.ANOMALY,
                    NOTIFICATION_TITLE_CAUTION,
                    String.format(NOTIFICATION_BODY_CAUTION, merchant.getName(), request.getAmount()),
                    NOTIFICATION_REF_TYPE_TRANSACTION,
                    transaction.getTransactionId()
                );
            }
        }

        return ExecuteResult.success(new ExecuteResponse(
            transaction.getTransactionId(),
            TRANSACTION_STATUS_COMPLETED,
            request.getAmount(),
            merchant.getName(),
            formatIso(paidAt)
        ));
    }

    /**
     * 판정 재료(walletId)는 비잠금 선조회로 얻는다 — 진행의 최종 기준은 ③의 잠금 조회이고,
     * 여기 결과는 판정에만 쓴다. 진행 가능성이 있는 건(PENDING)만 판정하며, 그 외 상태는
     * 판정 없이 ③의 최종 확인이 종결한다.
     */
    private FdsDecision evaluateFds(ExecuteRequest request, Merchant merchant) {
        PaymentRequest preview = paymentRequestMapper.findByToken(request.getQrToken());
        if (preview == null || preview.getStatus() != PaymentRequestStatus.PENDING) {
            return null;
        }
        return paymentFdsEvaluationService.evaluate(
            preview.getWalletId(), request.getAmount(), merchant);
    }

    /**
     * DANGER 차단 경로 — 잔액은 건드리지 않고 증적만 남긴다. BLOCKED 거래 행이 먼저인 이유는
     * 판정 기록과 알림이 거래 ID 를 참조하기 때문이다. balance_after 는 NULL 로 둬서 잔액
     * 무변동을 데이터로도 표현한다. 오류 응답은 증적이 커밋으로 남아야 하므로 잔액 부족과 같은
     * 커밋 후 던지기 패턴을 쓴다.
     */
    private ExecuteResult blockPayment(PaymentRequest paymentRequest, Merchant merchant,
        Long amount, FdsDecision fdsDecision) {
        Transaction transaction = Transaction.builder()
            .walletId(paymentRequest.getWalletId())
            .merchantId(merchant.getMerchantId())
            .type(TransactionType.PAYMENT)
            .amount(amount)
            .status(TransactionStatus.BLOCKED)
            .latitude(merchant.getLatitude().doubleValue())
            .longitude(merchant.getLongitude().doubleValue())
            .createdAt(LocalDateTime.now())
            .build();
        transactionMapper.insertTransaction(transaction);

        paymentFdsResultService.save(transaction.getTransactionId(), fdsDecision);
        paymentRequestMapper.failPayment(paymentRequest.getPaymentId(), FAILURE_CODE_FDS_BLOCKED);
        notificationService.notifyGuardians(
            paymentRequest.getSeniorId(),
            NotificationType.ANOMALY,
            NOTIFICATION_TITLE_BLOCKED,
            String.format(NOTIFICATION_BODY_BLOCKED, merchant.getName(), amount),
            NOTIFICATION_REF_TYPE_TRANSACTION,
            transaction.getTransactionId()
        );
        return ExecuteResult.failure(fdsBlockedException());
    }

    /** 완료 건의 멱등 응답 — findByToken의 merchants·transactions JOIN 결과로 조립한다 */
    private ExecuteResponse settledResponse(PaymentRequest settledView) {
        return new ExecuteResponse(
            settledView.getTransactionId(),
            settledView.getStatus().name(),
            settledView.getAmount(),
            settledView.getMerchantName(),
            formatIso(settledView.getPaidAt())
        );
    }

    /** 실패로 종결된 건의 재시도 — 최초 실패와 동일한 오류를 멱등 반환한다 */
    private BusinessException settledFailureException(String failureCode) {
        if (FAILURE_CODE_INSUFFICIENT_BALANCE.equals(failureCode)) {
            return new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, "WALLET_003", MESSAGE_INSUFFICIENT_BALANCE);
        }
        if (FAILURE_CODE_FDS_BLOCKED.equals(failureCode)) {
            return fdsBlockedException();
        }
        return new BusinessException(HttpStatus.BAD_REQUEST, "결제에 실패했습니다.");
    }

    private BusinessException invalidTokenException() {
        return new BusinessException(HttpStatus.BAD_REQUEST, "PAY_001", MESSAGE_INVALID_TOKEN);
    }

    private BusinessException fdsBlockedException() {
        return new BusinessException(HttpStatus.FORBIDDEN, "PAYMENT_006", MESSAGE_FDS_BLOCKED);
    }

    /** DB DATETIME(KST 기준)을 명세 형식(ISO 8601 +09:00 오프셋 포함)으로 변환 */
    private String formatIso(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.atZone(ZONE_SEOUL).toOffsetDateTime()
            .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

    /** 트랜잭션 결과 — error가 있으면 상태 기록을 커밋한 뒤 트랜잭션 밖에서 던진다 */
    private static class ExecuteResult {

        private final ExecuteResponse response;
        private final BusinessException error;

        private ExecuteResult(ExecuteResponse response, BusinessException error) {
            this.response = response;
            this.error = error;
        }

        private static ExecuteResult success(ExecuteResponse response) {
            return new ExecuteResult(response, null);
        }

        private static ExecuteResult failure(BusinessException error) {
            return new ExecuteResult(null, error);
        }
    }
}
