export type TransferStatus =
  'HELD' | 'COMPLETED' | 'REJECTED' | 'CANCELED' | 'EXPIRED' | 'FAILED'

export interface TransferRiskReason {
  code: string
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
  recipientName: string
  bankCode: string
  bankName: string
  accountNumber: string
  amount: number
  memo: string | null
  requestedAt: string
  approvalExpiresAt: string
  respondedAt: string | null
  completedAt: string | null
  remainingBalance: number | null
  riskAnalysis: TransferRiskAnalysis
  failureCode: string | null
  failureMessage: string | null
}

export interface TransferCancelResult {
  transactionId: number
  status: 'CANCELED'
}
