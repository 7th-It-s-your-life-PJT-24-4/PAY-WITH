package com.paywith.transfer.service;

import com.paywith.transfer.dto.PreparedTransfer;
import com.paywith.transfer.dto.TransferRequest;

public interface TransferPreparationService {
    PreparedTransfer prepare(Long userId, TransferRequest request);
}
