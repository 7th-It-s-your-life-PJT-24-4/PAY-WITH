import { apiClient } from '@/api/client'
import {
  guardTransactionDetailResponseSchema,
  guardTransactionHistoryResponseSchema,
  type GuardTransactionDetail,
  type GuardTransactionHistory,
  type TransactionRiskLevel,
} from '@/schemas/transaction.schema'

export interface GuardTransactionHistoryParams {
  type?: 'CHARGE' | 'PAYMENT' | 'TRANSFER'
  riskLevel?: TransactionRiskLevel
  page?: number
  size?: number
}

function createSearchParams(params: GuardTransactionHistoryParams) {
  const searchParams = new URLSearchParams()

  for (const [key, value] of Object.entries(params)) {
    if (value !== undefined) searchParams.set(key, String(value))
  }

  const query = searchParams.toString()
  return query ? `?${query}` : ''
}

export async function getGuardTransactionHistory(
  wardId: number,
  params: GuardTransactionHistoryParams = {},
): Promise<GuardTransactionHistory> {
  const response = await apiClient.get(
    `/guard/wards/${wardId}/transactions${createSearchParams(params)}`,
    guardTransactionHistoryResponseSchema,
  )
  return response.data
}

export async function getGuardTransactionDetail(
  wardId: number,
  transactionId: number,
): Promise<GuardTransactionDetail> {
  const response = await apiClient.get(
    `/guard/wards/${wardId}/transactions/${transactionId}`,
    guardTransactionDetailResponseSchema,
  )
  return response.data
}
