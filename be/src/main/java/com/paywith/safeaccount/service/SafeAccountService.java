package com.paywith.safeaccount.service;

import com.paywith.safeaccount.dto.*;

public interface SafeAccountService {

    // 피보호자 안전 계좌 등록 (송금 이력이 있는 경우 - 보호자 등록에서도 같이 사용)
    SafeAccountResponse registerByWard(Long wardId, SafeAccountRegisterRequest request);

    // 보호자가 피호보자 안전 계좌 (송금 이력이 없는 경우 - 보호자만 사용가능)
    SafeAccountResponse registerByGuard(Long guardId, Long wardId, GuardSafeAccountRegisterRequest request);

    // 피보호자가 안전 계좌를 조회
    SafeAccountListResponse getSafeAccountListByWard(Long wardId);

    // 보호자가 피보호자의 안전 계좌를 조회
    SafeAccountListResponse getSafeAccountListByGuard(Long guardId, Long wardId);

    // 피보호자가 자신의 안전 계좌 삭제(비활성화)
    SafeAccountDeleteResponse deactivateByWard(Long wardId, Long safeAccountId);

    // 보호자가 피보호자의 안전 계좌 삭제(비활성화)
    SafeAccountDeleteResponse deactivateByGuard(Long guardId, Long wardId, Long safeAccountId);
}
