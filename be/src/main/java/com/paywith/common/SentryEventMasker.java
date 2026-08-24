package com.paywith.common;

import io.sentry.Breadcrumb;
import io.sentry.Hint;
import io.sentry.SentryEvent;
import io.sentry.SentryOptions;
import io.sentry.protocol.Message;
import io.sentry.protocol.Request;
import io.sentry.protocol.SentryException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Sentry 로 전송되기 직전의 이벤트에서 값이 실릴 수 있는 자리를 {@link SensitiveDataMasker} 로
 * 훑는다. SentryOptions.beforeSend 로 등록된다(SentryConfig).
 *
 * <p>훑는 자리는 우리가 문자열을 만들어 넣는 곳뿐이다 — 로그 메시지, 예외 메시지, 브레드크럼,
 * 쿼리스트링. 헤더·쿠키·요청 본문은 sendDefaultPii=false / maxRequestBodySize=NONE 이라 애초에
 * 수집되지 않고, 스택트레이스는 값이 실리지 않으므로 그대로 둔다.
 */
public class SentryEventMasker implements SentryOptions.BeforeSendCallback {

    @Override
    public SentryEvent execute(SentryEvent event, Hint hint) {
        maskMessage(event.getMessage());
        maskExceptions(event.getExceptions());
        maskBreadcrumbs(event.getBreadcrumbs());
        maskRequest(event.getRequest());
        return event;
    }

    private void maskMessage(Message message) {
        if (message == null) {
            return;
        }
        message.setMessage(SensitiveDataMasker.mask(message.getMessage()));
        message.setFormatted(SensitiveDataMasker.mask(message.getFormatted()));

        List<String> params = message.getParams();
        if (params != null) {
            message.setParams(params.stream()
                .map(SensitiveDataMasker::mask)
                .collect(Collectors.toList()));
        }
    }

    private void maskExceptions(List<SentryException> exceptions) {
        if (exceptions == null) {
            return;
        }
        // 타입·모듈·스택트레이스는 그대로 둔다. 값이 실리는 자리는 value(예외 메시지)뿐이다.
        for (SentryException exception : exceptions) {
            exception.setValue(SensitiveDataMasker.mask(exception.getValue()));
        }
    }

    private void maskBreadcrumbs(List<Breadcrumb> breadcrumbs) {
        if (breadcrumbs == null) {
            return;
        }
        // INFO 로그가 브레드크럼으로 붙는다(logback.xml). 에러 자체보다 앞선 흐름이라 값이
        // 들어 있을 여지가 더 크다.
        for (Breadcrumb breadcrumb : breadcrumbs) {
            breadcrumb.setMessage(SensitiveDataMasker.mask(breadcrumb.getMessage()));
        }
    }

    private void maskRequest(Request request) {
        if (request == null) {
            return;
        }
        request.setQueryString(SensitiveDataMasker.mask(request.getQueryString()));
    }
}
