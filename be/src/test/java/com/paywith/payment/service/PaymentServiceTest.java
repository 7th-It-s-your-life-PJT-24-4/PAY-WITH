package com.paywith.payment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.paywith.exception.BusinessException;
import com.paywith.payment.domain.PaymentRequest;
import com.paywith.payment.domain.PaymentRequestStatus;
import com.paywith.payment.domain.PaymentWallet;
import com.paywith.payment.dto.PaymentCancelResponse;
import com.paywith.payment.dto.PaymentStatusResponse;
import com.paywith.payment.dto.QrCreateResponse;
import com.paywith.payment.mapper.PaymentRequestMapper;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    private static final long WARD_ID = 9001L;
    private static final long WALLET_ID = 11L;
    private static final long PAYMENT_ID = 42L;
    private static final String PIN = "123456";
    private static final String ENCODED_PIN = "{bcrypt}encoded-pin";

    @Mock
    private PaymentRequestMapper paymentRequestMapper;

    @Mock
    private PaymentTokenStore paymentTokenStore;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private PaymentService paymentService;

    // ---------- QR 생성 ----------

    @Test
    void createQr_GUARD_역할이면_403() {
        given(paymentRequestMapper.findUserRole(WARD_ID)).willReturn("GUARD");

        assertThatThrownBy(() -> paymentService.createQr(WARD_ID, PIN))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("status", HttpStatus.FORBIDDEN)
            .hasMessage("피보호자만 접근할 수 있습니다.");
    }

    @Test
    void createQr_사용자_없으면_403() {
        given(paymentRequestMapper.findUserRole(WARD_ID)).willReturn(null);

        assertThatThrownBy(() -> paymentService.createQr(WARD_ID, PIN))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("status", HttpStatus.FORBIDDEN);
    }

    @Test
    void createQr_페어링_미완료면_403_WARD_001() {
        given(paymentRequestMapper.findUserRole(WARD_ID)).willReturn("SENIOR");
        given(paymentRequestMapper.existsActivePairing(WARD_ID)).willReturn(false);

        assertThatThrownBy(() -> paymentService.createQr(WARD_ID, PIN))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("status", HttpStatus.FORBIDDEN)
            .hasMessage("페어링 완료 후 이용할 수 있습니다.");
    }

    @Test
    void createQr_지갑_없으면_404_WALLET_001() {
        given(paymentRequestMapper.findUserRole(WARD_ID)).willReturn("SENIOR");
        given(paymentRequestMapper.existsActivePairing(WARD_ID)).willReturn(true);
        given(paymentRequestMapper.findWalletByUserId(WARD_ID)).willReturn(null);

        assertThatThrownBy(() -> paymentService.createQr(WARD_ID, PIN))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND)
            .hasMessage("지갑 정보를 찾을 수 없습니다.");
    }

    @Test
    void createQr_지갑_LOCKED면_409_WALLET_002() {
        given(paymentRequestMapper.findUserRole(WARD_ID)).willReturn("SENIOR");
        given(paymentRequestMapper.existsActivePairing(WARD_ID)).willReturn(true);
        given(paymentRequestMapper.findWalletByUserId(WARD_ID)).willReturn(wallet("LOCKED", 130000L));

        assertThatThrownBy(() -> paymentService.createQr(WARD_ID, PIN))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("status", HttpStatus.CONFLICT)
            .hasMessage("현재 거래가 제한된 지갑입니다.");
    }

    @Test
    void createQr_결제_비밀번호_불일치면_400_PAYMENT_005() {
        given(paymentRequestMapper.findUserRole(WARD_ID)).willReturn("SENIOR");
        given(paymentRequestMapper.existsActivePairing(WARD_ID)).willReturn(true);
        given(paymentRequestMapper.findWalletByUserId(WARD_ID)).willReturn(wallet("ACTIVE", 130000L));
        given(passwordEncoder.matches(PIN, ENCODED_PIN)).willReturn(false);

        assertThatThrownBy(() -> paymentService.createQr(WARD_ID, PIN))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST)
            .hasMessage("결제 비밀번호가 올바르지 않습니다.");
        verify(paymentRequestMapper, never()).insert(any(PaymentRequest.class));
    }

    @Test
    void createQr_성공시_토큰생성_INSERT_Redis저장_순서로_응답() {
        given(paymentRequestMapper.findUserRole(WARD_ID)).willReturn("SENIOR");
        given(paymentRequestMapper.existsActivePairing(WARD_ID)).willReturn(true);
        given(paymentRequestMapper.findWalletByUserId(WARD_ID)).willReturn(wallet("ACTIVE", 130000L));
        given(passwordEncoder.matches(PIN, ENCODED_PIN)).willReturn(true);
        willAnswer(invocation -> {
            PaymentRequest inserted = invocation.getArgument(0);
            inserted.setPaymentId(PAYMENT_ID);
            return 1;
        }).given(paymentRequestMapper).insert(any(PaymentRequest.class));

        LocalDateTime before = LocalDateTime.now();
        QrCreateResponse response = paymentService.createQr(WARD_ID, PIN);
        LocalDateTime after = LocalDateTime.now();

        ArgumentCaptor<PaymentRequest> insertCaptor = ArgumentCaptor.forClass(PaymentRequest.class);
        verify(paymentRequestMapper).insert(insertCaptor.capture());
        PaymentRequest inserted = insertCaptor.getValue();
        assertThat(inserted.getWalletId()).isEqualTo(WALLET_ID);
        assertThat(inserted.getSeniorId()).isEqualTo(WARD_ID);
        assertThat(inserted.getStatus()).isEqualTo(PaymentRequestStatus.PENDING);
        // 토큰 형식: pay_qr_ + 랜덤 16자 (명세 — 개인정보 비포함 일회용 토큰)
        assertThat(inserted.getQrToken()).startsWith("pay_qr_").hasSize("pay_qr_".length() + 16);
        // 만료 시각: 생성 시점 + 60초
        assertThat(inserted.getExpiresAt()).isBetween(before.plusSeconds(60), after.plusSeconds(60));

        verify(paymentTokenStore).save(inserted.getQrToken(), PAYMENT_ID);

        assertThat(response.getPaymentId()).isEqualTo(PAYMENT_ID);
        assertThat(response.getQrToken()).isEqualTo(inserted.getQrToken());
        assertThat(response.getAvailableBalance()).isEqualTo(130000L);
        assertThat(response.getExpiresInSeconds()).isEqualTo(60);
        assertThat(response.getExpiresAt()).endsWith("+09:00");
    }

    // ---------- 상태 폴링 ----------

    @Test
    void getStatus_없는_결제요청이면_404_PAYMENT_002() {
        given(paymentRequestMapper.findUserRole(WARD_ID)).willReturn("SENIOR");
        given(paymentRequestMapper.findById(PAYMENT_ID)).willReturn(null);

        assertThatThrownBy(() -> paymentService.getStatus(WARD_ID, PAYMENT_ID))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND)
            .hasMessage("결제 요청을 찾을 수 없습니다.");
    }

    @Test
    void getStatus_타인의_결제요청이면_404_PAYMENT_002() {
        given(paymentRequestMapper.findUserRole(WARD_ID)).willReturn("SENIOR");
        PaymentRequest othersRequest = pendingRequest(LocalDateTime.now().plusSeconds(30));
        othersRequest.setSeniorId(7777L);
        given(paymentRequestMapper.findById(PAYMENT_ID)).willReturn(othersRequest);

        assertThatThrownBy(() -> paymentService.getStatus(WARD_ID, PAYMENT_ID))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND);
    }

    @Test
    void getStatus_PENDING인데_만료시각이_지났으면_EXPIRED로_lazy_전이() {
        given(paymentRequestMapper.findUserRole(WARD_ID)).willReturn("SENIOR");
        given(paymentRequestMapper.findById(PAYMENT_ID))
            .willReturn(pendingRequest(LocalDateTime.now().minusSeconds(1)));
        given(paymentRequestMapper.markExpiredIfPending(PAYMENT_ID)).willReturn(1);

        PaymentStatusResponse response = paymentService.getStatus(WARD_ID, PAYMENT_ID);

        verify(paymentRequestMapper).markExpiredIfPending(PAYMENT_ID);
        assertThat(response.getStatus()).isEqualTo("EXPIRED");
    }

    @Test
    void getStatus_PENDING이고_만료전이면_전이없이_PENDING_응답() {
        given(paymentRequestMapper.findUserRole(WARD_ID)).willReturn("SENIOR");
        PaymentRequest request = pendingRequest(LocalDateTime.now().plusSeconds(30));
        given(paymentRequestMapper.findById(PAYMENT_ID)).willReturn(request);

        PaymentStatusResponse response = paymentService.getStatus(WARD_ID, PAYMENT_ID);

        verify(paymentRequestMapper, never()).markExpiredIfPending(anyLong());
        assertThat(response.getPaymentId()).isEqualTo(PAYMENT_ID);
        assertThat(response.getStatus()).isEqualTo("PENDING");
        assertThat(response.getTransactionId()).isNull();
        assertThat(response.getMerchantName()).isNull();
        assertThat(response.getAmount()).isNull();
        assertThat(response.getPaidAt()).isNull();
        assertThat(response.getRemainingBalance()).isNull();
        assertThat(response.getFailureCode()).isNull();
        assertThat(response.getFailureMessage()).isNull();
        assertThat(response.getExpiresAt()).endsWith("+09:00");
    }

    @Test
    void getStatus_FAILED_잔액부족이면_실패_문구_포함() {
        given(paymentRequestMapper.findUserRole(WARD_ID)).willReturn("SENIOR");
        PaymentRequest request = pendingRequest(LocalDateTime.now().minusSeconds(10));
        request.setStatus(PaymentRequestStatus.FAILED);
        request.setFailureCode("INSUFFICIENT_BALANCE");
        given(paymentRequestMapper.findById(PAYMENT_ID)).willReturn(request);

        PaymentStatusResponse response = paymentService.getStatus(WARD_ID, PAYMENT_ID);

        assertThat(response.getStatus()).isEqualTo("FAILED");
        assertThat(response.getFailureCode()).isEqualTo("INSUFFICIENT_BALANCE");
        assertThat(response.getFailureMessage()).isEqualTo("결제 가능한 잔액이 부족합니다.");
    }

    // ---------- 취소 ----------

    @Test
    void cancel_PENDING이면_취소하고_Redis_토큰을_삭제() {
        given(paymentRequestMapper.findUserRole(WARD_ID)).willReturn("SENIOR");
        PaymentRequest pending = pendingRequest(LocalDateTime.now().plusSeconds(30));
        PaymentRequest canceled = pendingRequest(LocalDateTime.now().plusSeconds(30));
        canceled.setStatus(PaymentRequestStatus.CANCELED);
        canceled.setUpdatedAt(LocalDateTime.of(2026, 7, 16, 14, 45, 30));
        given(paymentRequestMapper.findById(PAYMENT_ID)).willReturn(pending, canceled);
        given(paymentRequestMapper.cancelIfPending(PAYMENT_ID)).willReturn(1);

        PaymentCancelResponse response = paymentService.cancel(WARD_ID, PAYMENT_ID);

        verify(paymentTokenStore).delete(pending.getQrToken());
        assertThat(response.getPaymentId()).isEqualTo(PAYMENT_ID);
        assertThat(response.getStatus()).isEqualTo("CANCELED");
        assertThat(response.getCanceledAt()).isEqualTo("2026-07-16T14:45:30+09:00");
    }

    @Test
    void cancel_PROCESSING이면_409_PAYMENT_003() {
        assertCancelConflict(PaymentRequestStatus.PROCESSING, "이미 처리 중이거나 완료된 결제는 취소할 수 없습니다.");
    }

    @Test
    void cancel_COMPLETED면_409_PAYMENT_003() {
        assertCancelConflict(PaymentRequestStatus.COMPLETED, "이미 처리 중이거나 완료된 결제는 취소할 수 없습니다.");
    }

    @Test
    void cancel_EXPIRED면_409_PAYMENT_004() {
        assertCancelConflict(PaymentRequestStatus.EXPIRED, "이미 만료된 결제 요청입니다.");
    }

    @Test
    void cancel_PENDING인데_만료시각이_지났으면_전이_후_409_PAYMENT_004() {
        given(paymentRequestMapper.findUserRole(WARD_ID)).willReturn("SENIOR");
        given(paymentRequestMapper.findById(PAYMENT_ID))
            .willReturn(pendingRequest(LocalDateTime.now().minusSeconds(1)));
        given(paymentRequestMapper.markExpiredIfPending(PAYMENT_ID)).willReturn(1);

        assertThatThrownBy(() -> paymentService.cancel(WARD_ID, PAYMENT_ID))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("status", HttpStatus.CONFLICT)
            .hasMessage("이미 만료된 결제 요청입니다.");
        verify(paymentRequestMapper, never()).cancelIfPending(anyLong());
    }

    @Test
    void cancel_이미_CANCELED면_멱등하게_성공_응답() {
        given(paymentRequestMapper.findUserRole(WARD_ID)).willReturn("SENIOR");
        PaymentRequest canceled = pendingRequest(LocalDateTime.now().plusSeconds(30));
        canceled.setStatus(PaymentRequestStatus.CANCELED);
        canceled.setUpdatedAt(LocalDateTime.of(2026, 7, 16, 14, 45, 30));
        given(paymentRequestMapper.findById(PAYMENT_ID)).willReturn(canceled);

        PaymentCancelResponse response = paymentService.cancel(WARD_ID, PAYMENT_ID);

        assertThat(response.getStatus()).isEqualTo("CANCELED");
        verify(paymentRequestMapper, never()).cancelIfPending(anyLong());
    }

    @Test
    void cancel_경합으로_조건부_취소가_실패하면_최신_상태로_재판정() {
        given(paymentRequestMapper.findUserRole(WARD_ID)).willReturn("SENIOR");
        PaymentRequest pending = pendingRequest(LocalDateTime.now().plusSeconds(30));
        PaymentRequest processing = pendingRequest(LocalDateTime.now().plusSeconds(30));
        processing.setStatus(PaymentRequestStatus.PROCESSING);
        given(paymentRequestMapper.findById(PAYMENT_ID)).willReturn(pending, processing);
        given(paymentRequestMapper.cancelIfPending(PAYMENT_ID)).willReturn(0);

        assertThatThrownBy(() -> paymentService.cancel(WARD_ID, PAYMENT_ID))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("status", HttpStatus.CONFLICT)
            .hasMessage("이미 처리 중이거나 완료된 결제는 취소할 수 없습니다.");
    }

    private void assertCancelConflict(PaymentRequestStatus status, String expectedMessage) {
        given(paymentRequestMapper.findUserRole(WARD_ID)).willReturn("SENIOR");
        PaymentRequest request = pendingRequest(LocalDateTime.now().plusSeconds(30));
        request.setStatus(status);
        given(paymentRequestMapper.findById(PAYMENT_ID)).willReturn(request);

        assertThatThrownBy(() -> paymentService.cancel(WARD_ID, PAYMENT_ID))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("status", HttpStatus.CONFLICT)
            .hasMessage(expectedMessage);
    }

    private PaymentWallet wallet(String status, Long balance) {
        PaymentWallet wallet = new PaymentWallet();
        wallet.setWalletId(WALLET_ID);
        wallet.setBalance(balance);
        wallet.setStatus(status);
        wallet.setPin(ENCODED_PIN);
        return wallet;
    }

    private PaymentRequest pendingRequest(LocalDateTime expiresAt) {
        PaymentRequest request = new PaymentRequest();
        request.setPaymentId(PAYMENT_ID);
        request.setWalletId(WALLET_ID);
        request.setSeniorId(WARD_ID);
        request.setQrToken("pay_qr_test0000000000");
        request.setStatus(PaymentRequestStatus.PENDING);
        request.setExpiresAt(expiresAt);
        request.setCreatedAt(LocalDateTime.now().minusSeconds(1));
        request.setUpdatedAt(LocalDateTime.now().minusSeconds(1));
        return request;
    }
}
