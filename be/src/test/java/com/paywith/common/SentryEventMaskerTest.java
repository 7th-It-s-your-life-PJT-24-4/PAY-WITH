package com.paywith.common;

import static org.assertj.core.api.Assertions.assertThat;

import io.sentry.Breadcrumb;
import io.sentry.Hint;
import io.sentry.SentryEvent;
import io.sentry.protocol.Message;
import io.sentry.protocol.Request;
import io.sentry.protocol.SentryException;
import io.sentry.protocol.SentryStackTrace;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Sentry 이벤트 마스킹")
class SentryEventMaskerTest {

    private static final String ACCOUNT_NO = "110234567890";

    private final SentryEventMasker masker = new SentryEventMasker();

    @Test
    void execute_masksLogMessageAndParams() {
        SentryEvent event = new SentryEvent();
        Message message = new Message();
        message.setMessage("수취인 계좌 {} 조회 실패");
        message.setFormatted("수취인 계좌 " + ACCOUNT_NO + " 조회 실패");
        message.setParams(Collections.singletonList(ACCOUNT_NO));
        event.setMessage(message);

        masker.execute(event, new Hint());

        assertThat(event.getMessage().getFormatted()).doesNotContain(ACCOUNT_NO);
        assertThat(event.getMessage().getParams()).containsExactly(SensitiveDataMasker.MASK);
    }

    @Test
    void execute_masksExceptionMessageButKeepsTypeAndStackTrace() {
        SentryEvent event = new SentryEvent();
        SentryException exception = new SentryException();
        exception.setType("DuplicateKeyException");
        exception.setValue("Duplicate entry '" + ACCOUNT_NO + "' for key 'accounts.account_no'");
        SentryStackTrace stackTrace = new SentryStackTrace();
        exception.setStacktrace(stackTrace);
        event.setExceptions(Collections.singletonList(exception));

        masker.execute(event, new Hint());

        SentryException masked = event.getExceptions().get(0);
        assertThat(masked.getValue()).doesNotContain(ACCOUNT_NO);
        // 원인 추적에 필요한 정보는 그대로 남아야 한다.
        assertThat(masked.getType()).isEqualTo("DuplicateKeyException");
        assertThat(masked.getStacktrace()).isSameAs(stackTrace);
    }

    @Test
    void execute_masksBreadcrumbMessage() {
        SentryEvent event = new SentryEvent();
        Breadcrumb breadcrumb = new Breadcrumb();
        breadcrumb.setMessage("송금 요청 수신. 계좌 " + ACCOUNT_NO);
        event.setBreadcrumbs(List.of(breadcrumb));

        masker.execute(event, new Hint());

        assertThat(event.getBreadcrumbs()).isNotNull();
        assertThat(event.getBreadcrumbs().get(0).getMessage()).doesNotContain(ACCOUNT_NO);
    }

    @Test
    void execute_masksRequestQueryString() {
        SentryEvent event = new SentryEvent();
        Request request = new Request();
        request.setUrl("https://paywith.site/api/accounts");
        request.setQueryString("accountNo=" + ACCOUNT_NO);
        event.setRequest(request);

        masker.execute(event, new Hint());

        assertThat(event.getRequest().getQueryString()).doesNotContain(ACCOUNT_NO);
        assertThat(event.getRequest().getUrl()).isEqualTo("https://paywith.site/api/accounts");
    }

    @Test
    void execute_returnsEventWhenNothingToMask() {
        SentryEvent event = new SentryEvent();

        assertThat(masker.execute(event, new Hint())).isSameAs(event);
    }
}
