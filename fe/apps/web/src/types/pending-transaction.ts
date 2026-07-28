interface PendingTransactionBase {
  transactionId: number
  status: 'HELD'
  amount: number
  requestedAt: string
  approvalExpiresAt: string
}

export interface PendingTransferTransaction extends PendingTransactionBase {
  type: 'TRANSFER'
  recipientName: string
  bankName: string
  accountNumber: string
}

export interface PendingPaymentTransaction extends PendingTransactionBase {
  type: 'PAYMENT'
  merchantName: string
  paymentMethod: string
}

export type PendingTransaction =
  PendingTransferTransaction | PendingPaymentTransaction
