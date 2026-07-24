export interface ChargeAccount {
  accountId: number
  bankCode: string
  bankName: string
  accountNumber: string
}

export interface RegisterChargeAccountRequest {
  bankCode: string
  accountNumber: string
  accountPassword: string
}

export interface ChargeResult {
  transactionId: number
  status: 'COMPLETED'
  chargedAmount: number
  balanceAfter: number
  bankName: string
  accountNumber: string
  createdAt: string
}

export type ChargeProcessingStatus = 'idle' | 'pending' | 'success' | 'error'
