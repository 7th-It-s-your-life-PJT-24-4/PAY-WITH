package com.paywith.payment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.paywith.payment.mapper.PaymentRequestMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaymentExpiryServiceTest {

    @Mock
    private PaymentRequestMapper paymentRequestMapper;

    @InjectMocks
    private PaymentExpiryService paymentExpiryService;

    @Test
    void expireOverdue_만료된_PENDING_건수를_반환() {
        given(paymentRequestMapper.expireOverdue()).willReturn(3);

        assertThat(paymentExpiryService.expireOverdue()).isEqualTo(3);
    }

    @Test
    void expireOverdue_만료_대상이_없으면_0을_반환() {
        given(paymentRequestMapper.expireOverdue()).willReturn(0);

        assertThat(paymentExpiryService.expireOverdue()).isZero();
    }

    @Test
    void expireOverdue_매퍼_실패는_전파되어_트랜잭션이_롤백된다() {
        given(paymentRequestMapper.expireOverdue())
            .willThrow(new IllegalStateException("db down"));

        assertThatThrownBy(() -> paymentExpiryService.expireOverdue())
            .isInstanceOf(IllegalStateException.class);
    }
}
