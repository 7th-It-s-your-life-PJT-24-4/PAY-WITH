package com.paywith.external.fraudcheck;

/**
 * 사기계좌 조회 포트. 실제 API(더치트 등)를 개인 개발자가 쓸 수 없어 현재는 목 구현만 있다.
 *
 * <p>구현체는 조회 실패를 삼키지 말고 예외로 알린다. fail-open 여부는 호출 측이 정할 문제다.
 */
public interface FraudAccountClient {

    boolean isReportedAsFraud(String bankCode, String accountNo);
}
