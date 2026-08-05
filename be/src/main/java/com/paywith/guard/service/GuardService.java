package com.paywith.guard.service;

import com.paywith.guard.dto.GuardPairingCodeResponse;
import com.paywith.guard.dto.WardPairingResponse;

public interface GuardService {
    boolean verifyGuardOfWard(Long guardId, Long wardId);

    /** 보호자가 페어링 코드+링크를 발급한다. 재발급하면 이전 코드는 즉시 무효화된다. */
    GuardPairingCodeResponse issuePairingCode(Long guardId);

    /** 피보호자가 코드를 검증해 보호자와 ACTIVE로 연동한다. 코드는 1회용으로 즉시 소비된다. */
    WardPairingResponse pairWithCode(Long wardId, String pairingCode);

    /** 보호자가 본인과 연결된 피보호자 관계를 해제한다. */
    void unpairWard(Long guardId, Long wardId);
}
