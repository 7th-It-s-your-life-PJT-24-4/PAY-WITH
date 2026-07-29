package com.paywith.external.fraudcheck;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("사기계좌 조회 목 구현")
class MockFraudAccountClientTest {

    private final MockFraudAccountClient client =
        new MockFraudAccountClient(new String[] {"088:110234567890", "090:3333012345678"});

    @Test
    void reportsRegisteredAccount() {
        assertThat(client.isReportedAsFraud("088", "110234567890")).isTrue();
    }

    @Test
    void ignoresHyphensInAccountNumber() {
        assertThat(client.isReportedAsFraud("088", "110-234-567890")).isTrue();
    }

    @Test
    void doesNotReportUnregisteredAccount() {
        assertThat(client.isReportedAsFraud("004", "999999999")).isFalse();
    }

    @Test
    void doesNotReportSameNumberAtDifferentBank() {
        assertThat(client.isReportedAsFraud("004", "110234567890")).isFalse();
    }

    @Test
    void doesNotReportWhenAccountIsNull() {
        assertThat(client.isReportedAsFraud("088", null)).isFalse();
        assertThat(client.isReportedAsFraud(null, "110234567890")).isFalse();
    }

    @Test
    void reportsNothingWhenMockListIsEmpty() {
        MockFraudAccountClient empty = new MockFraudAccountClient(new String[] {""});
        assertThat(empty.isReportedAsFraud("088", "110234567890")).isFalse();
    }
}
