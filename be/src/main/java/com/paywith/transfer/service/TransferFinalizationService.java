package com.paywith.transfer.service;

import com.paywith.fds.dto.FdsDecision;
import com.paywith.transfer.dto.PreparedTransfer;
import com.paywith.transfer.dto.TransferRequest;
import com.paywith.transfer.dto.TransferResponse;

public interface TransferFinalizationService {

    // 즉시 처리 메서드 (SAFE, CAUTION). 블랙리스트는 BLOCKED, 그 밖의 DANGER 는 HELD 로 끝난다
    TransferResponse finalize(PreparedTransfer prepared, FdsDecision decision, TransferRequest request);

    // 보호자 승인 후속 처리 (HELD 시점엔 PreparedTransfer가 없어서 승인 후 DB에서 다시 조회)
    TransferResponse finalizeApprovedTransfer(Long transactionId);
}
