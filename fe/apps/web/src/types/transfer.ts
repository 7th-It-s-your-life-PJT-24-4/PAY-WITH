export type TransferStatus =
  'HELD' | 'COMPLETED' | 'REJECTED' | 'CANCELED' | 'EXPIRED' | 'FAILED'

export interface TransferRiskReason {
  ruleCode: string
  description: string
  score: number
}

export interface TransferRiskAnalysis {
  riskScore: number
  reasons: TransferRiskReason[]
}

export interface TransferDetail {
  transactionId: number
  status: TransferStatus
  holderName: string
  bankCode: string
  bankName: string
  accountNo: string
  amount: number
  memo: string | null
  requestedAt: string
  expiredAt: string
  respondedAt: string | null
  completedAt: string | null
  balanceAfter: number | null
  riskAnalysis: TransferRiskAnalysis
  failureCode: string | null
  failureMessage: string | null
}

export interface TransferCancelResult {
  transactionId: number
  status: 'CANCELED'
}
