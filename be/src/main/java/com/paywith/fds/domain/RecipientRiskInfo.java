package com.paywith.fds.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * FDS 룰 평가에 필요한 수취인 위험 정보 조회 결과.
 */
@Getter
@Setter
@NoArgsConstructor
public class RecipientRiskInfo {

    private int sendCount;
    private boolean registeredSafe;
}
