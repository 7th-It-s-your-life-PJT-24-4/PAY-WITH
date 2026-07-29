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

    /**
     * 안전계좌를 등록한 주체(recipients.safe_registered_by). NULL 이면 시니어 본인 지정이다.
     *
     * <p>보호자가 등록한 계좌만 화이트리스트 단축평가 대상이고, 본인이 등록한 계좌는 감점에 그친다.
     * 본인 등록까지 우회로 인정하면 시니어를 설득해 대포통장을 안전계좌로 등록시키는 경로가 열린다.
     */
    private Long safeRegisteredBy;

    /** 보호자가 등록한 안전계좌인지 여부. */
    public boolean isGuardRegisteredSafe() {
        return registeredSafe && safeRegisteredBy != null;
    }

    /** 시니어 본인이 등록한 안전계좌인지 여부. */
    public boolean isSelfRegisteredSafe() {
        return registeredSafe && safeRegisteredBy == null;
    }
}
