interface PendingTransactionBase {
  transactionId: number
  status: 'HELD'
  amount: number
  requestedAt: string
  expiredAt: string
}

export interface PendingTransferTransaction extends PendingTransactionBase {
  type: 'TRANSFER'
  holderName: string
  bankName: string
  accountNo: string
}

export interface PendingPaymentTransaction extends PendingTransactionBase {
  type: 'PAYMENT'
  merchantName: string
  paymentMethod: string
}

export type PendingTransaction =
  PendingTransferTransaction | PendingPaymentTransaction
