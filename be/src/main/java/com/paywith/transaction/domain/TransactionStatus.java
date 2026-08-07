package com.paywith.transaction.domain;

public enum TransactionStatus {
    REQUESTED,
    HELD,
    APPROVED,
    PROCESSING,
    REJECTED,
    COMPLETED,
    CANCELED,
    BLOCKED,
    FAILED
}