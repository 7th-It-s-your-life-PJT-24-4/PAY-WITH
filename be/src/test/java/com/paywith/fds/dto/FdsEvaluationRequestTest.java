package com.paywith.fds.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 컨트롤러가 없어 @Valid 가 걸리지 않으므로 생성자에서 직접 방어한다.
 */
@DisplayName("FDS 평가 요청")
class FdsEvaluationRequestTest {

    private static final Long TX = 1L;
    private static final Long WALLET = 2L;
    private static final Long RECIPIENT = 3L;

    @Test
    void acceptsValidRequest() {
        FdsEvaluationRequest request =
            new FdsEvaluationRequest(TX, WALLET, RECIPIENT, new BigDecimal("30000"), "생일 축하");

        assertThat(request.getTransactionId()).isEqualTo(TX);
        assertThat(request.getMemo()).isEqualTo("생일 축하");
    }

    @Test
    void acceptsNullMemo() {
        FdsEvaluationRequest request =
            new FdsEvaluationRequest(TX, WALLET, RECIPIENT, new BigDecimal("30000"), null);

        assertThat(request.getMemo()).isNull();
    }

    // 없으면 속도 룰 집계에서 현재 거래를 제외할 수 없다
    @Test
    void rejectsNullTransactionId() {
        assertThatThrownBy(() ->
            new FdsEvaluationRequest(null, WALLET, RECIPIENT, new BigDecimal("30000"), null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("transactionId");
    }

    @Test
    void rejectsNullWalletId() {
        assertThatThrownBy(() ->
            new FdsEvaluationRequest(TX, null, RECIPIENT, new BigDecimal("30000"), null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("walletId");
    }

    @Test
    void rejectsNullRecipientId() {
        assertThatThrownBy(() ->
            new FdsEvaluationRequest(TX, WALLET, null, new BigDecimal("30000"), null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("recipientId");
    }

    // null 이면 금액 구간 룰에서 NPE 가 난다
    @Test
    void rejectsNullAmount() {
        assertThatThrownBy(() -> new FdsEvaluationRequest(TX, WALLET, RECIPIENT, null, null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("amount");
    }

    @Test
    void rejectsZeroAmount() {
        assertThatThrownBy(() ->
            new FdsEvaluationRequest(TX, WALLET, RECIPIENT, BigDecimal.ZERO, null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNegativeAmount() {
        assertThatThrownBy(() ->
            new FdsEvaluationRequest(TX, WALLET, RECIPIENT, new BigDecimal("-1000"), null))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
