package com.paywith.bank.domain;

import java.util.Arrays;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * 한국 금융결제원(KFTC) 표준 은행코드 기준 계좌번호 형식 규칙.
 *
 * <p>계좌번호 규칙 출처: 나무위키 "계좌번호" 문서 및 공개 금융 자료 기반 best-effort 정리.
 * 공식 규격은 KFTC 내부 문서로 비공개이며, 실계좌 검증은 오픈뱅킹 실명조회 API로 수행한다.
 *
 * <ul>
 *   <li>minLength / maxLength — 허용 계좌번호 자릿수 범위 (숫자만, 하이픈 제외)
 *   <li>prefixPattern — 계좌번호 시작 조건을 정규식으로 표현 (null이면 prefix 미검사)
 * </ul>
 */
public enum BankAccountRule {

    /** 한국산업은행 (002) — 11~12자리 */
    KDB("002", 11, 12, null),

    /** IBK기업은행 (003) — 14자리 */
    IBK("003", 14, 14, null),

    /**
     * KB국민은행 (004) — 10~14자리.
     * 합병 전 한국주택은행·국민은행(1963년) 구계좌 포함.
     */
    KB("004", 10, 14, null),

    /** Sh수협은행 (007) — 12자리 */
    SH("007", 12, 12, null),

    /**
     * NH농협은행 (011) — 11~13자리.
     * 단위농협(지역농축협)은 별도 은행코드를 사용하지 않으므로 동일 코드로 처리.
     * 신계좌(11자리)와 구계좌(12~13자리)를 모두 포함.
     */
    NH("011", 13, 13, "^(301|302|312)"),

    /** 우리은행 (020) — 13자리 */
    WOORI("020", 13, 13, null),

    /** SC제일은행 (023) — 12자리 */
    SC("023", 12, 12, null),

    /** 한국씨티은행 (027) — 10자리 */
    CITI("027", 10, 10, null),

    /** DGB대구은행(iM뱅크) (031) — 12자리 */
    DGB("031", 12, 12, null),

    /** BNK부산은행 (032) — 12자리 */
    BNK_BUSAN("032", 12, 12, null),

    /** 광주은행 (034) — 12자리 */
    GWANGJU("034", 12, 12, null),

    /** 제주은행 (035) — 12자리 */
    JEJU("035", 12, 12, null),

    /** 전북은행 (037) — 12자리 */
    JEONBUK("037", 12, 12, null),

    /** BNK경남은행 (039) — 12자리 */
    BNK_KYONGNAM("039", 12, 12, null),

    /**
     * 새마을금고 (045) — 13~14자리.
     * 구계좌 13자리, 신계좌 14자리 혼재.
     */
    MG("045", 13, 13, "^900[0-9]"),

    /**
     * 신협 (048) — 12~13자리.
     * 구계좌 13자리, 신계좌 12자리 혼재.
     */
    CU("048", 12, 13, null),

    /** 우체국예금보험 (071) — 13자리 */
    POST("071", 13, 13, null),

    /**
     * 하나은행 (081) — 11~14자리.
     * 합병 전 외환은행 구계좌 포함으로 범위가 넓다.
     */
    HANA("081", 11, 14, null),

    /**
     * 신한은행 (088) — 11~12자리.
     * 합병 전 조흥은행 구계좌(12자리) 및 신한 신계좌(11자리) 포함.
     */
    SHINHAN("088", 11, 12, null),

    /**
     * 케이뱅크 (089) — 12자리.
     * 계좌번호 과목코드: 1001, 1002.
     */
    KBANK("089", 12, 12, "^(1001|1002)"),

    /**
     * 카카오뱅크 (090) — 13자리.
     * 일반 계좌번호 고정 prefix: 3333.
     */
    KAKAO("090", 13, 13, "^3333"),

    /**
     * 토스뱅크 (092) — 12자리.
     * 계좌번호 과목코드: 1000, 1001.
     */
    TOSS("092", 12, 12, "^(1000|1001)");

    private final String bankCode;
    private final int minLength;
    private final int maxLength;
    /** null이면 prefix 조건을 적용하지 않는다. */
    private final Pattern prefixPattern;

    BankAccountRule(String bankCode, int minLength, int maxLength, String prefixRegex) {
        this.bankCode = bankCode;
        this.minLength = minLength;
        this.maxLength = maxLength;
        this.prefixPattern = (prefixRegex != null) ? Pattern.compile(prefixRegex) : null;
    }

    public String getBankCode() {
        return bankCode;
    }

    /**
     * 주어진 계좌번호가 이 은행의 형식 규칙을 충족하는지 검사한다.
     *
     * @param accountNo 숫자만으로 구성된 계좌번호 (하이픈 미포함)
     * @return 자릿수 범위 AND prefix 패턴(있을 경우) 모두 충족하면 {@code true}
     */
    public boolean matches(String accountNo) {
        if (accountNo == null) return false;
        if (!accountNo.matches("\\d+")) return false;
        int len = accountNo.length();
        if (len < minLength || len > maxLength) return false;
        if (prefixPattern != null && !prefixPattern.matcher(accountNo).find()) return false;
        return true;
    }

    /**
     * 은행코드로 규칙을 조회한다.
     *
     * @param bankCode 금결원 표준 은행코드 (예: "090")
     * @return 해당 코드의 규칙이 정의되어 있으면 {@link Optional}에 담아 반환, 없으면 empty
     */
    public static Optional<BankAccountRule> findByBankCode(String bankCode) {
        return Arrays.stream(values())
            .filter(r -> r.bankCode.equals(bankCode))
            .findFirst();
    }
}
