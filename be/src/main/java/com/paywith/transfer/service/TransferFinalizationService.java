package com.paywith.transfer.service;

import com.paywith.fds.domain.RiskLevel;
import com.paywith.transfer.dto.PreparedTransfer;
import com.paywith.transfer.dto.TransferRequest;
import com.paywith.transfer.dto.TransferResponse;

public interface TransferFinalizationService {
    TransferResponse finalize(PreparedTransfer prepared, RiskLevel riskLevel, TransferRequest request);
}
