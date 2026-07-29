package com.paywith.fds.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 판정에 필요한 수취인 정보만 뽑은 조회 결과. recipients 행 전체가 아니다. */
@Getter
@Setter
@NoArgsConstructor
public class RecipientRiskInfo {

    /** 사기계좌 조회 키. 같은 계좌도 시니어마다 다른 행이라 계좌번호로 조회한다. */
    private String bankCode;
    private String accountNo;

    private int sendCount;
    private boolean registeredSafe;

    /** NULL 이면 시니어 본인 지정, 값이 있으면 해당 보호자. */
    private Long safeRegisteredBy;
}
