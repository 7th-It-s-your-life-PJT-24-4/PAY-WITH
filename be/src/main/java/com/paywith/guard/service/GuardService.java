package com.paywith.guard.service;

import com.paywith.guard.dto.*;

public interface GuardService {
    boolean verifyGuardOfWard(Long guardId, Long wardId);

    /** 보호자가 페어링 코드+링크를 발급한다. 재발급하면 이전 코드는 즉시 무효화된다. */
    GuardPairingCodeResponse issuePairingCode(Long guardId);

    /** 피보호자가 코드를 검증해 보호자와 ACTIVE로 연동한다. 코드는 1회용으로 즉시 소비된다. */
    WardPairingResponse pairWithCode(Long wardId, String pairingCode);

    /** 보호자가 본인과 연결된 피보호자 관계를 해제한다. */
    void unpairWard(Long guardId, Long wardId);

    /** 피보호자가 자신과 연동된 보호자의 이름·전화번호·아바타를 조회한다. */
    GuardInfoResponse findMyGuardian(Long wardId);

    // 피보호자가 코드를 입력해 대기요청을 만들어 Redis에 PENDING으로 저장
    PairingRequestResponse createPairingRequest(Long wardId, String pairingCode);

    // 피보호자 화면이 폴링용으로 호출
    PairingStatusResponse checkPairingStatus(Long wardId, String requestId);

    // 보호자 화면이 폴링용으로 호출
    PendingPairingRequestResponse findPendingRequest(Long guardId);

    // 보호자가 확인 버튼을 눌렀을 때, 여기서 실제 연동 확정
    WardPairingResponse confirmPairingRequest(Long guardId, String requestId);
}
