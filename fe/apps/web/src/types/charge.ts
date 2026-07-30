export interface ChargeAccount {
  accountId: number
  bankCode: string
  bankName: string
  accountNo: string
}

export interface RegisterChargeAccountRequest {
  bankCode: string
  accountNo: string
  accountPassword: string
}

export interface ChargeResult {
  transactionId: number
  status: 'COMPLETED'
  chargeAmount: number
  balanceAfter: number
  bankName: string
  accountNo: string
  createdAt: string
}

export type ChargeProcessingStatus = 'idle' | 'pending' | 'success' | 'error'
