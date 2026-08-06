package com.paywith.payment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.paywith.exception.BusinessException;
import com.paywith.fds.domain.DecidedBy;
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
import com.paywith.transaction.mapper.TransactionMapper;
import com.paywith.wallet.mapper.WalletMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * 결제 실행 시퀀스 단위 테스트. PlatformTransactionManager는 목이므로 TransactionTemplate이
 * 콜백을 인라인 실행한다 — 커밋·롤백 경계는 목이 무시하고 비즈니스 규칙만 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class PaymentExecuteServiceTest {

    private static final long WARD_ID = 9001L;
    private static final long WALLET_ID = 11L;
    private static final long PAYMENT_ID = 42L;
    private static final long MERCHANT_ID = 1L;
    private static final long TRANSACTION_ID = 1024L;
    private static final long AMOUNT = 4500L;
    private static final String QR_TOKEN = "pay_qr_a8F2kL9xQ1mNzzzz";

    @Mock
    private PaymentRequestMapper paymentRequestMapper;

    @Mock
    private MerchantMapper merchantMapper;

    @Mock
    private WalletMapper walletMapper;

    @Mock
    private TransactionMapper transactionMapper;

    @Mock
    private PaymentTokenStore paymentTokenStore;

    @Mock
    private PaymentFdsEvaluationService paymentFdsEvaluationService;

    @Mock
    private PaymentFdsResultService paymentFdsResultService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private PlatformTransactionManager transactionManager;

    @InjectMocks
    private PaymentExecuteService paymentExecuteService;

    // ---------- 성공 경로 ----------

    @Test
    void execute_성공시_차감_원장기록_완료전이_후_명세_5필드_응답() {
        given(paymentTokenStore.consume(QR_TOKEN)).willReturn(PAYMENT_ID);
        given(merchantMapper.findById(MERCHANT_ID)).willReturn(merchant());
        given(paymentRequestMapper.findByTokenForUpdate(QR_TOKEN)).willReturn(pendingRequest());
        given(paymentRequestMapper.findWalletByUserId(WARD_ID))
            .willReturn(wallet(130000L), wallet(125500L));
        given(paymentRequestMapper.markProcessing(PAYMENT_ID, MERCHANT_ID, AMOUNT)).willReturn(1);
        given(walletMapper.decreaseBalanceIfSufficient(WALLET_ID, AMOUNT)).willReturn(1);
        willAnswer(invocation -> {
            invocation.<Transaction>getArgument(0).setTransactionId(TRANSACTION_ID);
            return null;
        }).given(transactionMapper).insertTransaction(any(Transaction.class));
        given(paymentRequestMapper.completePayment(PAYMENT_ID, TRANSACTION_ID)).willReturn(1);

        ExecuteResponse response = paymentExecuteService.execute(request());

        assertThat(response.getTransactionId()).isEqualTo(TRANSACTION_ID);
        assertThat(response.getStatus()).isEqualTo("COMPLETED");
        assertThat(response.getAmount()).isEqualTo(AMOUNT);
        assertThat(response.getMerchantName()).isEqualTo("GS25 강남역점");
        assertThat(response.getCreatedAt()).endsWith("+09:00");
    }

    @Test
    void execute_성공시_거래_원장에_서버조회_좌표와_balance_after를_기록() {
        given(paymentTokenStore.consume(QR_TOKEN)).willReturn(PAYMENT_ID);
        given(merchantMapper.findById(MERCHANT_ID)).willReturn(merchant());
        given(paymentRequestMapper.findByTokenForUpdate(QR_TOKEN)).willReturn(pendingRequest());
        given(paymentRequestMapper.findWalletByUserId(WARD_ID))
            .willReturn(wallet(130000L), wallet(125500L));
        given(paymentRequestMapper.markProcessing(PAYMENT_ID, MERCHANT_ID, AMOUNT)).willReturn(1);
        given(walletMapper.decreaseBalanceIfSufficient(WALLET_ID, AMOUNT)).willReturn(1);
        willAnswer(invocation -> {
            invocation.<Transaction>getArgument(0).setTransactionId(TRANSACTION_ID);
            return null;
        }).given(transactionMapper).insertTransaction(any(Transaction.class));
        given(paymentRequestMapper.completePayment(PAYMENT_ID, TRANSACTION_ID)).willReturn(1);

        paymentExecuteService.execute(request());

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionMapper).insertTransaction(captor.capture());
        Transaction transaction = captor.getValue();
        assertThat(transaction.getWalletId()).isEqualTo(WALLET_ID);
        assertThat(transaction.getMerchantId()).isEqualTo(MERCHANT_ID);
        assertThat(transaction.getType()).isEqualTo("PAYMENT");
        assertThat(transaction.getStatus()).isEqualTo("COMPLETED");
        assertThat(transaction.getAmount()).isEqualTo(AMOUNT);
        assertThat(transaction.getBalanceAfter()).isEqualTo(125500L);
        assertThat(transaction.getLatitude()).isEqualTo(37.4979520d);
        assertThat(transaction.getLongitude()).isEqualTo(127.0276190d);
        assertThat(transaction.getInitiatedBy()).isNull();
        // created_at은 DB DEFAULT에 맡기지 않고 서비스가 명시 세팅한다
        assertThat(transaction.getCreatedAt()).isNotNull();
        assertThat(transaction.getCompletedAt()).isEqualTo(transaction.getCreatedAt());
    }

    // ---------- FDS 판정 위치 (②′) ----------

    @Test
    void execute_FDS_판정은_행잠금_전에_선조회한_walletId로_호출() {
        given(paymentTokenStore.consume(QR_TOKEN)).willReturn(PAYMENT_ID);
        Merchant merchant = merchant();
        given(merchantMapper.findById(MERCHANT_ID)).willReturn(merchant);
        given(paymentRequestMapper.findByToken(QR_TOKEN)).willReturn(pendingRequest());
        given(paymentRequestMapper.findByTokenForUpdate(QR_TOKEN)).willReturn(pendingRequest());
        given(paymentRequestMapper.findWalletByUserId(WARD_ID))
            .willReturn(wallet(130000L), wallet(125500L));
        given(paymentRequestMapper.markProcessing(PAYMENT_ID, MERCHANT_ID, AMOUNT)).willReturn(1);
        given(walletMapper.decreaseBalanceIfSufficient(WALLET_ID, AMOUNT)).willReturn(1);
        willAnswer(invocation -> {
            invocation.<Transaction>getArgument(0).setTransactionId(TRANSACTION_ID);
            return null;
        }).given(transactionMapper).insertTransaction(any(Transaction.class));
        given(paymentRequestMapper.completePayment(PAYMENT_ID, TRANSACTION_ID)).willReturn(1);

        paymentExecuteService.execute(request());

        // 잠금 보유 시간 최소화 원칙 — 판정이 FOR UPDATE 조회보다 먼저여야 한다
        InOrder order = inOrder(paymentFdsEvaluationService, paymentRequestMapper);
        order.verify(paymentFdsEvaluationService).evaluate(WALLET_ID, AMOUNT, merchant);
        order.verify(paymentRequestMapper).findByTokenForUpdate(QR_TOKEN);
    }

    @Test
    void execute_선조회_행이_없으면_FDS_판정_건너뜀() {
        given(paymentTokenStore.consume(QR_TOKEN)).willReturn(PAYMENT_ID);
        given(merchantMapper.findById(MERCHANT_ID)).willReturn(merchant());
        given(paymentRequestMapper.findByToken(QR_TOKEN)).willReturn(null);
        given(paymentRequestMapper.findByTokenForUpdate(QR_TOKEN)).willReturn(null);

        assertThatThrownBy(() -> paymentExecuteService.execute(request()))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("code", "PAY_001");
        verifyNoInteractions(paymentFdsEvaluationService);
    }

    @Test
    void execute_선조회가_종결_상태면_FDS_판정_건너뜀() {
        given(paymentTokenStore.consume(QR_TOKEN)).willReturn(PAYMENT_ID);
        given(merchantMapper.findById(MERCHANT_ID)).willReturn(merchant());
        given(paymentRequestMapper.findByToken(QR_TOKEN)).willReturn(completedView());
        PaymentRequest lockedView = pendingRequest();
        lockedView.setStatus(PaymentRequestStatus.COMPLETED);
        given(paymentRequestMapper.findByTokenForUpdate(QR_TOKEN)).willReturn(lockedView);

        ExecuteResponse response = paymentExecuteService.execute(request());

        assertThat(response.getStatus()).isEqualTo("COMPLETED");
        verifyNoInteractions(paymentFdsEvaluationService);
    }

    // ---------- FDS 판정 분기 (④) ----------

    @Test
    void execute_DANGER면_차감_없이_403_차단_응답() {
        given(paymentTokenStore.consume(QR_TOKEN)).willReturn(PAYMENT_ID);
        Merchant merchant = merchant();
        given(merchantMapper.findById(MERCHANT_ID)).willReturn(merchant);
        given(paymentRequestMapper.findByToken(QR_TOKEN)).willReturn(pendingRequest());
        given(paymentFdsEvaluationService.evaluate(WALLET_ID, AMOUNT, merchant))
            .willReturn(decision(RiskLevel.DANGER));
        given(paymentRequestMapper.findByTokenForUpdate(QR_TOKEN)).willReturn(pendingRequest());
        given(paymentRequestMapper.findWalletByUserId(WARD_ID)).willReturn(wallet(130000L));
        given(paymentRequestMapper.markProcessing(PAYMENT_ID, MERCHANT_ID, AMOUNT)).willReturn(1);

        assertThatThrownBy(() -> paymentExecuteService.execute(request()))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("status", HttpStatus.FORBIDDEN)
            .hasFieldOrPropertyWithValue("code", "PAYMENT_006")
            .hasMessage("결제가 차단되었습니다. 보호자에게 문의해 주세요.");
        verifyNoInteractions(walletMapper);
        verify(paymentRequestMapper).failPayment(PAYMENT_ID, "FDS_BLOCKED");
        verify(paymentRequestMapper, never()).completePayment(anyLong(), anyLong());
    }

    @Test
    void execute_DANGER면_잔액_무변동_BLOCKED_증적과_판정_기록을_남긴다() {
        given(paymentTokenStore.consume(QR_TOKEN)).willReturn(PAYMENT_ID);
        Merchant merchant = merchant();
        given(merchantMapper.findById(MERCHANT_ID)).willReturn(merchant);
        given(paymentRequestMapper.findByToken(QR_TOKEN)).willReturn(pendingRequest());
        FdsDecision danger = decision(RiskLevel.DANGER);
        given(paymentFdsEvaluationService.evaluate(WALLET_ID, AMOUNT, merchant))
            .willReturn(danger);
        given(paymentRequestMapper.findByTokenForUpdate(QR_TOKEN)).willReturn(pendingRequest());
        given(paymentRequestMapper.findWalletByUserId(WARD_ID)).willReturn(wallet(130000L));
        given(paymentRequestMapper.markProcessing(PAYMENT_ID, MERCHANT_ID, AMOUNT)).willReturn(1);
        willAnswer(invocation -> {
            invocation.<Transaction>getArgument(0).setTransactionId(TRANSACTION_ID);
            return null;
        }).given(transactionMapper).insertTransaction(any(Transaction.class));

        assertThatThrownBy(() -> paymentExecuteService.execute(request()))
            .isInstanceOf(BusinessException.class);

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionMapper).insertTransaction(captor.capture());
        Transaction transaction = captor.getValue();
        assertThat(transaction.getStatus()).isEqualTo("BLOCKED");
        assertThat(transaction.getType()).isEqualTo("PAYMENT");
        assertThat(transaction.getWalletId()).isEqualTo(WALLET_ID);
        assertThat(transaction.getMerchantId()).isEqualTo(MERCHANT_ID);
        assertThat(transaction.getAmount()).isEqualTo(AMOUNT);
        // 잔액 무변동 — balance_after NULL, 완료 시각 없음
        assertThat(transaction.getBalanceAfter()).isNull();
        assertThat(transaction.getCompletedAt()).isNull();
        assertThat(transaction.getLatitude()).isEqualTo(37.4979520d);
        assertThat(transaction.getLongitude()).isEqualTo(127.0276190d);
        verify(paymentFdsResultService).save(TRANSACTION_ID, danger);
    }

    @Test
    void execute_DANGER면_보호자에게_차단_통지를_남긴다() {
        given(paymentTokenStore.consume(QR_TOKEN)).willReturn(PAYMENT_ID);
        Merchant merchant = merchant();
        given(merchantMapper.findById(MERCHANT_ID)).willReturn(merchant);
        given(paymentRequestMapper.findByToken(QR_TOKEN)).willReturn(pendingRequest());
        given(paymentFdsEvaluationService.evaluate(WALLET_ID, AMOUNT, merchant))
            .willReturn(decision(RiskLevel.DANGER));
        given(paymentRequestMapper.findByTokenForUpdate(QR_TOKEN)).willReturn(pendingRequest());
        given(paymentRequestMapper.findWalletByUserId(WARD_ID)).willReturn(wallet(130000L));
        given(paymentRequestMapper.markProcessing(PAYMENT_ID, MERCHANT_ID, AMOUNT)).willReturn(1);
        willAnswer(invocation -> {
            invocation.<Transaction>getArgument(0).setTransactionId(TRANSACTION_ID);
            return null;
        }).given(transactionMapper).insertTransaction(any(Transaction.class));

        assertThatThrownBy(() -> paymentExecuteService.execute(request()))
            .isInstanceOf(BusinessException.class);

        verify(notificationService).notifyGuardians(WARD_ID, NotificationType.ANOMALY,
            "결제 차단 알림", "위험 거래가 감지되어 GS25 강남역점 4,500원 결제를 차단했습니다.",
            "TRANSACTION", TRANSACTION_ID);
    }

    @Test
    void execute_CAUTION이면_정상_완료_후_기록과_주의_알림() {
        given(paymentTokenStore.consume(QR_TOKEN)).willReturn(PAYMENT_ID);
        Merchant merchant = merchant();
        given(merchantMapper.findById(MERCHANT_ID)).willReturn(merchant);
        given(paymentRequestMapper.findByToken(QR_TOKEN)).willReturn(pendingRequest());
        FdsDecision caution = decision(RiskLevel.CAUTION);
        given(paymentFdsEvaluationService.evaluate(WALLET_ID, AMOUNT, merchant))
            .willReturn(caution);
        given(paymentRequestMapper.findByTokenForUpdate(QR_TOKEN)).willReturn(pendingRequest());
        given(paymentRequestMapper.findWalletByUserId(WARD_ID))
            .willReturn(wallet(130000L), wallet(125500L));
        given(paymentRequestMapper.markProcessing(PAYMENT_ID, MERCHANT_ID, AMOUNT)).willReturn(1);
        given(walletMapper.decreaseBalanceIfSufficient(WALLET_ID, AMOUNT)).willReturn(1);
        willAnswer(invocation -> {
            invocation.<Transaction>getArgument(0).setTransactionId(TRANSACTION_ID);
            return null;
        }).given(transactionMapper).insertTransaction(any(Transaction.class));
        given(paymentRequestMapper.completePayment(PAYMENT_ID, TRANSACTION_ID)).willReturn(1);

        ExecuteResponse response = paymentExecuteService.execute(request());

        assertThat(response.getStatus()).isEqualTo("COMPLETED");
        verify(paymentFdsResultService).save(TRANSACTION_ID, caution);
        verify(notificationService).notifyGuardians(WARD_ID, NotificationType.ANOMALY,
            "결제 주의 알림",
            "주의가 필요한 결제가 감지되었습니다. GS25 강남역점 4,500원 결제가 정상 완료되었습니다.",
            "TRANSACTION", TRANSACTION_ID);
    }

    @Test
    void execute_SAFE면_판정_기록만_남기고_알림_없음() {
        given(paymentTokenStore.consume(QR_TOKEN)).willReturn(PAYMENT_ID);
        Merchant merchant = merchant();
        given(merchantMapper.findById(MERCHANT_ID)).willReturn(merchant);
        given(paymentRequestMapper.findByToken(QR_TOKEN)).willReturn(pendingRequest());
        FdsDecision safe = decision(RiskLevel.SAFE);
        given(paymentFdsEvaluationService.evaluate(WALLET_ID, AMOUNT, merchant))
            .willReturn(safe);
        given(paymentRequestMapper.findByTokenForUpdate(QR_TOKEN)).willReturn(pendingRequest());
        given(paymentRequestMapper.findWalletByUserId(WARD_ID))
            .willReturn(wallet(130000L), wallet(125500L));
        given(paymentRequestMapper.markProcessing(PAYMENT_ID, MERCHANT_ID, AMOUNT)).willReturn(1);
        given(walletMapper.decreaseBalanceIfSufficient(WALLET_ID, AMOUNT)).willReturn(1);
        willAnswer(invocation -> {
            invocation.<Transaction>getArgument(0).setTransactionId(TRANSACTION_ID);
            return null;
        }).given(transactionMapper).insertTransaction(any(Transaction.class));
        given(paymentRequestMapper.completePayment(PAYMENT_ID, TRANSACTION_ID)).willReturn(1);

        paymentExecuteService.execute(request());

        verify(paymentFdsResultService).save(TRANSACTION_ID, safe);
        verifyNoInteractions(notificationService);
    }

    @Test
    void execute_판정이_없으면_기록도_알림도_없음() {
        given(paymentTokenStore.consume(QR_TOKEN)).willReturn(PAYMENT_ID);
        given(merchantMapper.findById(MERCHANT_ID)).willReturn(merchant());
        given(paymentRequestMapper.findByToken(QR_TOKEN)).willReturn(null);
        given(paymentRequestMapper.findByTokenForUpdate(QR_TOKEN)).willReturn(pendingRequest());
        given(paymentRequestMapper.findWalletByUserId(WARD_ID))
            .willReturn(wallet(130000L), wallet(125500L));
        given(paymentRequestMapper.markProcessing(PAYMENT_ID, MERCHANT_ID, AMOUNT)).willReturn(1);
        given(walletMapper.decreaseBalanceIfSufficient(WALLET_ID, AMOUNT)).willReturn(1);
        willAnswer(invocation -> {
            invocation.<Transaction>getArgument(0).setTransactionId(TRANSACTION_ID);
            return null;
        }).given(transactionMapper).insertTransaction(any(Transaction.class));
        given(paymentRequestMapper.completePayment(PAYMENT_ID, TRANSACTION_ID)).willReturn(1);

        ExecuteResponse response = paymentExecuteService.execute(request());

        assertThat(response.getStatus()).isEqualTo("COMPLETED");
        verifyNoInteractions(paymentFdsEvaluationService, paymentFdsResultService, notificationService);
    }

    @Test
    void execute_Redis미스_FDS차단건은_최초와_동일한_403을_멱등_반환() {
        given(paymentTokenStore.consume(QR_TOKEN)).willReturn(null);
        PaymentRequest blockedView = pendingRequest();
        blockedView.setStatus(PaymentRequestStatus.FAILED);
        blockedView.setFailureCode("FDS_BLOCKED");
        given(paymentRequestMapper.findByToken(QR_TOKEN)).willReturn(blockedView);

        assertThatThrownBy(() -> paymentExecuteService.execute(request()))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("status", HttpStatus.FORBIDDEN)
            .hasFieldOrPropertyWithValue("code", "PAYMENT_006")
            .hasMessage("결제가 차단되었습니다. 보호자에게 문의해 주세요.");
        verifyNoInteractions(merchantMapper, walletMapper, transactionMapper,
            paymentFdsEvaluationService, paymentFdsResultService, notificationService);
    }

    // ---------- 멱등 경로 (Redis 미스 → MySQL 폴백) ----------

    @Test
    void execute_Redis미스_COMPLETED건은_기존_결과를_멱등_반환() {
        given(paymentTokenStore.consume(QR_TOKEN)).willReturn(null);
        given(paymentRequestMapper.findByToken(QR_TOKEN)).willReturn(completedView());

        ExecuteResponse response = paymentExecuteService.execute(request());

        assertThat(response.getTransactionId()).isEqualTo(TRANSACTION_ID);
        assertThat(response.getStatus()).isEqualTo("COMPLETED");
        assertThat(response.getAmount()).isEqualTo(AMOUNT);
        assertThat(response.getMerchantName()).isEqualTo("GS25 강남역점");
        assertThat(response.getCreatedAt()).endsWith("+09:00");
        verify(paymentRequestMapper, never()).findByTokenForUpdate(anyString());
        verifyNoInteractions(merchantMapper, walletMapper, transactionMapper);
    }

    @Test
    void execute_Redis미스_FAILED건은_최초와_동일한_400을_멱등_반환() {
        given(paymentTokenStore.consume(QR_TOKEN)).willReturn(null);
        PaymentRequest failedView = pendingRequest();
        failedView.setStatus(PaymentRequestStatus.FAILED);
        failedView.setFailureCode("INSUFFICIENT_BALANCE");
        given(paymentRequestMapper.findByToken(QR_TOKEN)).willReturn(failedView);

        assertThatThrownBy(() -> paymentExecuteService.execute(request()))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("status", HttpStatus.UNPROCESSABLE_ENTITY)
            .hasFieldOrPropertyWithValue("code", "WALLET_003")
            .hasMessage("결제 가능한 잔액이 부족합니다.");
        verifyNoInteractions(merchantMapper, walletMapper, transactionMapper);
    }

    @Test
    void execute_Redis미스_PENDING_미만료건은_MySQL_기준으로_정상_진행() {
        given(paymentTokenStore.consume(QR_TOKEN)).willReturn(null);
        given(paymentRequestMapper.findByToken(QR_TOKEN)).willReturn(pendingRequest());
        given(merchantMapper.findById(MERCHANT_ID)).willReturn(merchant());
        given(paymentRequestMapper.findByTokenForUpdate(QR_TOKEN)).willReturn(pendingRequest());
        given(paymentRequestMapper.findWalletByUserId(WARD_ID))
            .willReturn(wallet(130000L), wallet(125500L));
        given(paymentRequestMapper.markProcessing(PAYMENT_ID, MERCHANT_ID, AMOUNT)).willReturn(1);
        given(walletMapper.decreaseBalanceIfSufficient(WALLET_ID, AMOUNT)).willReturn(1);
        willAnswer(invocation -> {
            invocation.<Transaction>getArgument(0).setTransactionId(TRANSACTION_ID);
            return null;
        }).given(transactionMapper).insertTransaction(any(Transaction.class));
        given(paymentRequestMapper.completePayment(PAYMENT_ID, TRANSACTION_ID)).willReturn(1);

        ExecuteResponse response = paymentExecuteService.execute(request());

        assertThat(response.getStatus()).isEqualTo("COMPLETED");
    }

    @Test
    void execute_잠금_직후_다른_실행이_먼저_완료한_경우도_멱등_반환() {
        given(paymentTokenStore.consume(QR_TOKEN)).willReturn(PAYMENT_ID);
        given(merchantMapper.findById(MERCHANT_ID)).willReturn(merchant());
        PaymentRequest lockedView = pendingRequest();
        lockedView.setStatus(PaymentRequestStatus.COMPLETED);
        given(paymentRequestMapper.findByTokenForUpdate(QR_TOKEN)).willReturn(lockedView);
        given(paymentRequestMapper.findByToken(QR_TOKEN)).willReturn(completedView());

        ExecuteResponse response = paymentExecuteService.execute(request());

        assertThat(response.getTransactionId()).isEqualTo(TRANSACTION_ID);
        verifyNoInteractions(walletMapper, transactionMapper);
    }

    // ---------- 무효 토큰 ----------

    @Test
    void execute_Redis미스_행_없으면_400_무효토큰() {
        given(paymentTokenStore.consume(QR_TOKEN)).willReturn(null);
        given(paymentRequestMapper.findByToken(QR_TOKEN)).willReturn(null);

        assertThatThrownBy(() -> paymentExecuteService.execute(request()))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST)
            .hasFieldOrPropertyWithValue("code", "PAY_001")
            .hasMessage("유효하지 않은 QR입니다. 새 QR로 다시 시도해주세요.");
        verifyNoInteractions(merchantMapper, walletMapper, transactionMapper);
    }

    @Test
    void execute_Redis미스_PENDING_만료건은_lazy_EXPIRED_전이_후_400() {
        given(paymentTokenStore.consume(QR_TOKEN)).willReturn(null);
        PaymentRequest expiredPending = pendingRequest();
        expiredPending.setExpiresAt(LocalDateTime.now().minusSeconds(1));
        given(paymentRequestMapper.findByToken(QR_TOKEN)).willReturn(expiredPending);

        assertThatThrownBy(() -> paymentExecuteService.execute(request()))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("code", "PAY_001")
            .hasMessage("유효하지 않은 QR입니다. 새 QR로 다시 시도해주세요.");
        verify(paymentRequestMapper).markExpiredIfPending(PAYMENT_ID);
        verifyNoInteractions(merchantMapper, walletMapper, transactionMapper);
    }

    @Test
    void execute_Redis미스_CANCELED건은_400_무효토큰() {
        given(paymentTokenStore.consume(QR_TOKEN)).willReturn(null);
        PaymentRequest canceledView = pendingRequest();
        canceledView.setStatus(PaymentRequestStatus.CANCELED);
        given(paymentRequestMapper.findByToken(QR_TOKEN)).willReturn(canceledView);

        assertThatThrownBy(() -> paymentExecuteService.execute(request()))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("code", "PAY_001")
            .hasMessage("유효하지 않은 QR입니다. 새 QR로 다시 시도해주세요.");
    }

    // ---------- 잠금 후 최종 확인 ----------

    @Test
    void execute_잠금_시점에_행_없으면_400_무효토큰() {
        given(paymentTokenStore.consume(QR_TOKEN)).willReturn(PAYMENT_ID);
        given(merchantMapper.findById(MERCHANT_ID)).willReturn(merchant());
        given(paymentRequestMapper.findByTokenForUpdate(QR_TOKEN)).willReturn(null);

        assertThatThrownBy(() -> paymentExecuteService.execute(request()))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("code", "PAY_001")
            .hasMessage("유효하지 않은 QR입니다. 새 QR로 다시 시도해주세요.");
        verifyNoInteractions(walletMapper, transactionMapper);
    }

    @Test
    void execute_잠금_후_PROCESSING이면_400_무효토큰() {
        given(paymentTokenStore.consume(QR_TOKEN)).willReturn(PAYMENT_ID);
        given(merchantMapper.findById(MERCHANT_ID)).willReturn(merchant());
        PaymentRequest processingView = pendingRequest();
        processingView.setStatus(PaymentRequestStatus.PROCESSING);
        given(paymentRequestMapper.findByTokenForUpdate(QR_TOKEN)).willReturn(processingView);

        assertThatThrownBy(() -> paymentExecuteService.execute(request()))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("code", "PAY_001")
            .hasMessage("유효하지 않은 QR입니다. 새 QR로 다시 시도해주세요.");
        verifyNoInteractions(walletMapper, transactionMapper);
    }

    @Test
    void execute_잠금_후_만료_경과면_EXPIRED_전이_기록_후_400() {
        given(paymentTokenStore.consume(QR_TOKEN)).willReturn(PAYMENT_ID);
        given(merchantMapper.findById(MERCHANT_ID)).willReturn(merchant());
        PaymentRequest expiredPending = pendingRequest();
        expiredPending.setExpiresAt(LocalDateTime.now().minusSeconds(1));
        given(paymentRequestMapper.findByTokenForUpdate(QR_TOKEN)).willReturn(expiredPending);

        assertThatThrownBy(() -> paymentExecuteService.execute(request()))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("code", "PAY_001")
            .hasMessage("유효하지 않은 QR입니다. 새 QR로 다시 시도해주세요.");
        verify(paymentRequestMapper).markExpiredIfPending(PAYMENT_ID);
        verifyNoInteractions(walletMapper, transactionMapper);
    }

    @Test
    void execute_지갑_소유자_불일치면_400_무효토큰_차감_안함() {
        given(paymentTokenStore.consume(QR_TOKEN)).willReturn(PAYMENT_ID);
        given(merchantMapper.findById(MERCHANT_ID)).willReturn(merchant());
        given(paymentRequestMapper.findByTokenForUpdate(QR_TOKEN)).willReturn(pendingRequest());
        PaymentWallet otherWallet = wallet(130000L);
        otherWallet.setWalletId(99L);
        given(paymentRequestMapper.findWalletByUserId(WARD_ID)).willReturn(otherWallet);

        assertThatThrownBy(() -> paymentExecuteService.execute(request()))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("code", "PAY_001")
            .hasMessage("유효하지 않은 QR입니다. 새 QR로 다시 시도해주세요.");
        verifyNoInteractions(walletMapper, transactionMapper);
    }

    @Test
    void execute_PROCESSING_전이_경합으로_0건이면_400_무효토큰() {
        given(paymentTokenStore.consume(QR_TOKEN)).willReturn(PAYMENT_ID);
        given(merchantMapper.findById(MERCHANT_ID)).willReturn(merchant());
        given(paymentRequestMapper.findByTokenForUpdate(QR_TOKEN)).willReturn(pendingRequest());
        given(paymentRequestMapper.findWalletByUserId(WARD_ID)).willReturn(wallet(130000L));
        given(paymentRequestMapper.markProcessing(PAYMENT_ID, MERCHANT_ID, AMOUNT)).willReturn(0);

        assertThatThrownBy(() -> paymentExecuteService.execute(request()))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("code", "PAY_001")
            .hasMessage("유효하지 않은 QR입니다. 새 QR로 다시 시도해주세요.");
        verifyNoInteractions(walletMapper, transactionMapper);
    }

    // ---------- 가맹점 검증 ----------

    @Test
    void execute_가맹점_미존재면_404() {
        given(paymentTokenStore.consume(QR_TOKEN)).willReturn(PAYMENT_ID);
        given(merchantMapper.findById(MERCHANT_ID)).willReturn(null);

        assertThatThrownBy(() -> paymentExecuteService.execute(request()))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND)
            .hasFieldOrPropertyWithValue("code", "MERCHANT_001")
            .hasMessage("가맹점을 찾을 수 없습니다.");
        verify(paymentRequestMapper, never()).findByTokenForUpdate(anyString());
        verifyNoInteractions(walletMapper, transactionMapper);
    }

    @Test
    void execute_좌표_미등록_가맹점이면_404_결제_거부() {
        given(paymentTokenStore.consume(QR_TOKEN)).willReturn(PAYMENT_ID);
        Merchant noCoordinates = merchant();
        noCoordinates.setLatitude(null);
        noCoordinates.setLongitude(null);
        given(merchantMapper.findById(MERCHANT_ID)).willReturn(noCoordinates);

        assertThatThrownBy(() -> paymentExecuteService.execute(request()))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND)
            .hasFieldOrPropertyWithValue("code", "MERCHANT_001")
            .hasMessage("결제할 수 없는 가맹점입니다.");
        verify(paymentRequestMapper, never()).findByTokenForUpdate(anyString());
    }

    // ---------- 잔액 부족 ----------

    @Test
    void execute_잔액_부족이면_FAILED_기록_후_400() {
        given(paymentTokenStore.consume(QR_TOKEN)).willReturn(PAYMENT_ID);
        given(merchantMapper.findById(MERCHANT_ID)).willReturn(merchant());
        given(paymentRequestMapper.findByTokenForUpdate(QR_TOKEN)).willReturn(pendingRequest());
        given(paymentRequestMapper.findWalletByUserId(WARD_ID)).willReturn(wallet(3000L));
        given(paymentRequestMapper.markProcessing(PAYMENT_ID, MERCHANT_ID, AMOUNT)).willReturn(1);
        given(walletMapper.decreaseBalanceIfSufficient(WALLET_ID, AMOUNT)).willReturn(0);

        assertThatThrownBy(() -> paymentExecuteService.execute(request()))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("status", HttpStatus.UNPROCESSABLE_ENTITY)
            .hasFieldOrPropertyWithValue("code", "WALLET_003")
            .hasMessage("결제 가능한 잔액이 부족합니다.");
        verify(paymentRequestMapper).failPayment(PAYMENT_ID, "INSUFFICIENT_BALANCE");
        verify(paymentRequestMapper, never()).completePayment(anyLong(), anyLong());
        verifyNoInteractions(transactionMapper);
    }

    // ---------- 헬퍼 ----------

    private static ExecuteRequest request() {
        ExecuteRequest request = new ExecuteRequest();
        request.setQrToken(QR_TOKEN);
        request.setMerchantId(MERCHANT_ID);
        request.setAmount(AMOUNT);
        return request;
    }

    private static Merchant merchant() {
        Merchant merchant = new Merchant();
        merchant.setMerchantId(MERCHANT_ID);
        merchant.setName("GS25 강남역점");
        merchant.setCategoryCode("CS");
        merchant.setRegion("강남");
        merchant.setLatitude(new BigDecimal("37.4979520"));
        merchant.setLongitude(new BigDecimal("127.0276190"));
        return merchant;
    }

    private static PaymentRequest pendingRequest() {
        PaymentRequest paymentRequest = new PaymentRequest();
        paymentRequest.setPaymentId(PAYMENT_ID);
        paymentRequest.setWalletId(WALLET_ID);
        paymentRequest.setSeniorId(WARD_ID);
        paymentRequest.setQrToken(QR_TOKEN);
        paymentRequest.setStatus(PaymentRequestStatus.PENDING);
        paymentRequest.setExpiresAt(LocalDateTime.now().plusSeconds(40));
        return paymentRequest;
    }

    /** findByToken의 merchants·transactions JOIN 결과를 흉내 낸 완료 건 조회 뷰 */
    private static PaymentRequest completedView() {
        PaymentRequest view = pendingRequest();
        view.setStatus(PaymentRequestStatus.COMPLETED);
        view.setTransactionId(TRANSACTION_ID);
        view.setAmount(AMOUNT);
        view.setMerchantId(MERCHANT_ID);
        view.setMerchantName("GS25 강남역점");
        view.setPaidAt(LocalDateTime.of(2026, 7, 16, 15, 30, 0));
        view.setRemainingBalance(125500L);
        return view;
    }

    private static PaymentWallet wallet(long balance) {
        PaymentWallet wallet = new PaymentWallet();
        wallet.setWalletId(WALLET_ID);
        wallet.setBalance(balance);
        wallet.setStatus("ACTIVE");
        return wallet;
    }

    private static FdsDecision decision(RiskLevel riskLevel) {
        return new FdsDecision(riskLevel, DecidedBy.RULE, 60, 25, 50, Collections.emptyList());
    }
}
