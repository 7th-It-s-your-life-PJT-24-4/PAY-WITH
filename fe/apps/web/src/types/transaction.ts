export type TransactionType = 'PAYMENT' | 'TRANSFER'

export type TransactionDirection = 'CREDIT' | 'DEBIT'

export type TransactionRiskLevel = 'SAFE' | 'CAUTION' | 'BLOCKED'

export interface PaymentTransactionDetail {
  merchantName: string
}

export interface TransferTransactionDetail {
  recipientName: string
  bankName: string
  accountNumber: string
}

export interface WardTransaction {
  transactionId: number
  type: TransactionType
  direction: TransactionDirection
  title: string
  amount: number
  occurredAt: string
  methodLabel: string
  memo: string | null
  riskLevel: TransactionRiskLevel
  riskScore: number
  riskSummary: string
  riskReasons: string[]
  payment?: PaymentTransactionDetail
  transfer?: TransferTransactionDetail
}
